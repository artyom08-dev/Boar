package ac.boar.anticheat.util;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Vec3;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;

public class InputUtil {
    public static void processInput(final BoarPlayer player, final PlayerAuthInputPacket packet) {
        Vec3 input = Vec3.ZERO.clone();

        // pre-766 clients (1.21.50) don't have the raw move vector
        // remember it so other checks can fall back to the old input flags too
        if (packet.getRawMoveVector() == null) {
            player.legacyMovementProtocol = true;
        }

        // Player literally can't move in these cases.
        final Vector2f motion = packet.getMotion();
        if (player.doingInventoryAction || motion == null || motion.getX() == 0 && motion.getY() == 0) {
            player.input = input;
            return;
        }

        final Vector2f moveVector = resolveMoveVector(packet, motion);

        input = new Vec3(MathUtil.clamp(moveVector.getX(), -1, 1), 0, MathUtil.clamp(moveVector.getY(), -1, 1));
        if (MathUtil.sign(input.x) == input.x && MathUtil.sign(input.z) == input.z && input.x != 0 && input.z != 0) {
            // Avoid the use of sqrt if possible.
            input = input.multiply(0.70710677F);
        } else {
            float length = input.horizontalLength();
            // Player input should only be normalized if player won't gain any advantage after normalizing input.
            if (length >= 1) {
                input = new Vec3(input.x / length, 0, input.z / length);
            }
        }

        player.input = input;
    }

    // pre-766 clients don't send the raw move vector, so fall back to the analog one, then to motion
    private static Vector2f resolveMoveVector(final PlayerAuthInputPacket packet, final Vector2f motion) {
        final Vector2f rawMoveVector = packet.getRawMoveVector();
        if (rawMoveVector != null) {
            return rawMoveVector;
        }

        final Vector2f analogMoveVector = packet.getAnalogMoveVector();
        if (analogMoveVector != null && analogMoveVector.lengthSquared() > 0) {
            return analogMoveVector;
        }

        return motion;
    }
}
