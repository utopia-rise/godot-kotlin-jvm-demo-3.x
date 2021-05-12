import godot.*
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.Basis
import godot.core.VariantArray
import godot.core.Vector3
import godot.global.GD

@RegisterClass
class FollowCamera : Camera() {
    private val collisionExceptions = VariantArray<Any?>()

    @RegisterProperty
    var minDistance = 0.5

    @RegisterProperty
    var maxDistance = 3.5

    @RegisterProperty
    var angleVAdjust = 0.0

    @RegisterProperty
    var autoturnRayAperture = 25

    @RegisterProperty
    var autoturnSpeed = 50

    private val maxHeight = 2.0
    private val minHeight = 0.0

    @RegisterFunction
    override fun _physicsProcess(delta: Double) {
        val target = (getParent() as Spatial).globalTransform.origin
        var pos = globalTransform.origin
        val up = Vector3(0f, 1f, 0f)

        var diff = pos - target

        // check ranges
        if (diff.length() < minDistance) {
            diff = diff.normalized() * minDistance
        } else if (diff.length() > maxDistance) {
            diff = diff.normalized() * maxDistance
        }

        // check upper and lower height
        if (diff.y > maxHeight) {
            diff.y = maxHeight
        }
        if (diff.y < minHeight) {
            diff.y = minHeight
        }

        // Check autoturn
        val ds = PhysicsServer.spaceGetDirectState(getWorld()!!.space)!!

        val colLeft = ds.intersectRay(
            target,
            target + Basis(up, GD.deg2rad(autoturnRayAperture.toDouble())).xform(diff),
            collisionExceptions
        )
        val col = ds.intersectRay(target, target + diff, collisionExceptions)
        val colRight = ds.intersectRay(
            target,
            target + Basis(up, GD.deg2rad((-autoturnRayAperture).toDouble())).xform(diff),
            collisionExceptions
        )

        if (!col.empty()) {
            // If main ray was occluded, get camera closer, this is the worst case scenario
            diff = (col["position"] as Vector3) - target
        } else if (!colLeft.empty() && colRight.empty()) {
            // If only left ray is occluded, turn the camera around to the right
            diff = Basis(up, GD.deg2rad(-delta * autoturnSpeed)).xform(diff)
        } else if (colLeft.empty() && !colRight.empty()) {
            // If only right ray is occluded, turn the camera around to the left
            diff = Basis(up, GD.deg2rad(delta * autoturnSpeed)).xform(diff)
        } else {
            // Do nothing otherwise, left and right are occluded but center is not, so do not autoturn
        }

        // Apply lookat
        if (diff == Vector3()) {
            diff = (pos - target).normalized() * 0.0001f
        }

        pos = target + diff

        lookAtFromPosition(pos, target, up)

        // Turn a little up or down
        val t = transform
        t.basis = Basis(t.basis[0], GD.deg2rad(angleVAdjust)) * t.basis
        transform = t

        // alternative
        // transform {
        //  basis = Basis(t.basis[0], angleVAdjust.toRadians()) * t.basis
        // }
    }

    @RegisterFunction
    override fun _ready() {
        // Find collision exceptions for ray
        var node: Node? = this
        while (node != null) {
            if (node is RigidBody) {
                collisionExceptions.append(node.getRid())
                break
            } else {
                node = try {
                    node.getParent()
                } catch (e: Throwable) {
                    null
                }
            }
        }

        setPhysicsProcess(true)
        // This detaches the camera transform from the parent spatial node
        setAsToplevel(true)
    }
}
