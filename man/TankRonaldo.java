package man;
import robocode.*;
import robocode.util.Utils;

import java.awt.Color;

/**
 * TankRonaldo - a Robocode Robot created for the ASW course.
 *
 * @author Ekaterina Aksenova
 * @author Tomás Fontes
 * @version March 2024
 */
public class TankRonaldo extends AdvancedRobot {
    private int moveDirection = 1;
    private int scanDirection = 1;

    /**
     * TankRonaldo's default behavior in each round.
     * The radar is set to turn independently of the gun.
     * The gun is set to turn independently of the body.
     * Radar continuously scans for enemies in all directions.
     */
    public void run() {
        setBodyColor(Color.black);
        setGunColor(Color.lightGray);
        setRadarColor(Color.blue);
        setBulletColor(Color.red);

        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);

        while (true) {
            turnRadarRight(360);
        }
    }

    /**
     * Actions taken when another Robot is detected by the radar actioned in the {@link #run()} method.
     * Executes the queued up standard sequences of radar rotation, body movement, gun engagement.
     *
     * @param enemy information about the most recently scanned enemy Robot on the battlefield
     */
    public void onScannedRobot(ScannedRobotEvent enemy) {
        doRadar(enemy);
        doMove(enemy);
        doGun(enemy);
        execute();
    }

    /**
     * Normalizes a Robot's bearing to between -180 and +180 degrees.
     *
     * @param angle position of a Robot or its element in relation to North
     * @return      normalized angle in range (-180, 180) degrees
     */
    private double normalizeBearing(double angle) {
        while (angle >  180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    /**
     * Describes the radar behavior.
     * Oscillates 30 degrees in the detected enemy's direction, then reverses the orientation.
     *
     * @param enemy Robot captured by the {@link #onScannedRobot(ScannedRobotEvent)} method
     */
    public void doRadar(ScannedRobotEvent enemy) {
        double turnRadar = getHeading() - getRadarHeading() + enemy.getBearing();
        turnRadar += 30 * scanDirection;
        setTurnRadarRight(normalizeBearing(turnRadar));
        scanDirection *= -1;
    }

    /**
     * Describes the movement behavior.
     * Turns Robot's body almost perpendicular to the enemy, allowing to move towards it.
     * Strafes and closes in by changing direction every 20 ticks.
     * Also inverts direction upon collision with a wall or another Robot.
     *
     * @param enemy Robot captured by the {@link #onScannedRobot(ScannedRobotEvent)} method
     */
    public void doMove(ScannedRobotEvent enemy) {
        double enemyDirection = enemy.getBearing() + 90 - (15 * moveDirection);
        setTurnRight(normalizeBearing(enemyDirection));

        if (getTime() % 20 == 0 || getVelocity() == 0) {
            moveDirection *= -1;
            setAhead(150 * moveDirection);
        }
    }

    /**
     * Calculates optimal gun direction based on future enemy position.
     * Takes enemy's absolute bearing to the Robot's body;
     * sets enemy's future coordinates based on its velocity, heading, estimated bullet travel time towards it,
     * and current distance to it.
     *
     * @param enemy Robot captured by the {@link #onScannedRobot(ScannedRobotEvent)} method
     * @return      optimal gun angle; the difference between the angle to enemy's predicted <code>(x,y)</code>
     *              and current gun direction
     */
    private double getOptimalGunAngle(ScannedRobotEvent enemy) {
        double absoluteBearing = getHeadingRadians() + enemy.getBearingRadians();

        double enemyX = getX() + enemy.getDistance() * Math.sin(absoluteBearing);
        double enemyY = getY() + enemy.getDistance() * Math.cos(absoluteBearing);

        double bulletVelocity = 20 - (3 * Math.min(3, Math.abs(getGunTurnRemaining())));
        double deltaTime = enemy.getDistance() / bulletVelocity;

        enemyX += enemy.getVelocity() * Math.sin(enemy.getHeadingRadians()) * deltaTime;
        enemyY += enemy.getVelocity() * Math.cos(enemy.getHeadingRadians()) * deltaTime;

        double theta = Utils.normalAbsoluteAngle(Math.atan2(enemyX - getX(), enemyY - getY()));
        return Utils.normalRelativeAngle(theta - getGunHeadingRadians());
    }

    /**
     * Describes the gun behavior.
     * Turns gun to the position determined by the {@link #getOptimalGunAngle} method.
     * Adjusts firepower based on enemy proximity.
     * Fires if the gun is cool and pointed at the target within a reasonable margin.
     *
     * @param enemy Robot captured by the {@link #onScannedRobot(ScannedRobotEvent)} method
     */
    public void doGun(ScannedRobotEvent enemy) {
        double gunAngle = getOptimalGunAngle(enemy);
        setTurnGunRightRadians(gunAngle);

        double firePower = Math.min(400 / enemy.getDistance(), 3);

        if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10)
            setFire(firePower);
    }

    /**
     * Victory dance performed by the Robot in case of winning a round of a battle.
     *
     * @param victory round win captured by {@link #onWin(WinEvent)}
     */
    public void onWin(WinEvent victory) {
        for (int i = 0; i < 3; i++) {
            turnRight(360);
            turnLeft(360);
            for (int j = 0; j < 3; j++) {
                turnGunRight(180);
                turnRadarLeft(180);
                turnGunLeft(180);
                turnRadarRight(180);
            }
        }
    }
}