package theDeadlyDuo;
import robocode.*;
import robocode.Robot;
import robocode.Robot.*;
import robocode.TeamRobot.*;
import robocode.util.Utils;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.awt.*;
public class sparta extends TeamRobot {
	private static final double Bpower = 1.9;
	 
	private static double lDirection;
	private static double lastEnemyVelocity;
	private static GFTMovement movement;
 
	public sparta() {
		movement = new GFTMovement(this);	
	}
 
	public void run() {
		setColors(Color.BLACK, Color.BLACK, Color.BLACK);
		lDirection = 1;
		lastEnemyVelocity = 0;
		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);
		do {
			turnRadarRightRadians(Double.POSITIVE_INFINITY); 
		} while (true);
	}
 
	public void onScannedRobot(ScannedRobotEvent e) {
		if (e.getName() == "sparta*" && e.getName() == "MOObot*"){
		}
		else{
			double enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
			double enemyDistance = e.getDistance();
			double enemyVelocity = e.getVelocity();
			if (enemyVelocity != 0) {
				lDirection = GFTUtils.sign(enemyVelocity * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing));
			}
			GFTWave wave = new GFTWave(this);
			wave.gunLocation = new Point2D.Double(getX(), getY());
			GFTWave.targetLocation = GFTUtils.project(wave.gunLocation, enemyAbsoluteBearing, enemyDistance);
			wave.lDirection = lDirection;
			wave.bulletPower = Bpower;
			wave.setSegmentations(enemyDistance, enemyVelocity, lastEnemyVelocity);
			lastEnemyVelocity = enemyVelocity;
			wave.bearing = enemyAbsoluteBearing;
			setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getGunHeadingRadians() + wave.mostVisitedBearingOffset()));
			setFire(wave.bulletPower);
			if (getEnergy() >= Bpower) {
				addCustomEvent(wave);
			}
			movement.onScannedRobot(e);
			setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
		}
	}
}
 
class GFTWave extends Condition {
	static Point2D targetLocation;
 
	double bulletPower;
	Point2D gunLocation;
	double bearing;
	double lDirection;
 
	private static final double mostdistance = 1000;
	private static final int distanceList = 5;
	private static final int lovList = 5;
	private static final int bins = 25;
	private static final int middleBin = (bins - 1) / 2;
	private static final double maxEscapeAngles = 0.7;
	private static final double binWidth = maxEscapeAngles / (double)middleBin;
 
	private static int[][][][] statBuffers = new int[distanceList][lovList][lovList][bins];
 
	private int[] buffer;
	private AdvancedRobot robot;
	private double distanceTraveled;
 
	GFTWave(AdvancedRobot _robot) {
		this.robot = _robot;
	}
 
	public boolean test() {
		advance();
		if (hasArrived()) {
			buffer[currentBin()]++;
			robot.removeCustomEvent(this);
		}
		return false;
	}
 
	double mostVisitedBearingOffset() {
		return (lDirection * binWidth) * (mostVisitedBin() - middleBin);
	}
 
	void setSegmentations(double distance, double velocity, double lastVelocity) {
		int distanceIndex = (int)(distance / (mostdistance / distanceList));
		int velocityIndex = (int)Math.abs(velocity / 2);
		int lastVelocityIndex = (int)Math.abs(lastVelocity / 2);
		buffer = statBuffers[distanceIndex][velocityIndex][lastVelocityIndex];
	}
 
	private void advance() {
		distanceTraveled += GFTUtils.bulletVelocity(bulletPower);
	}
 
	private boolean hasArrived() {
		return distanceTraveled > gunLocation.distance(targetLocation) - 18;
	}
 
	private int currentBin() {
		int bin = (int)Math.round(((Utils.normalRelativeAngle(GFTUtils.absoluteBearing(gunLocation, targetLocation) - bearing)) /
				(lDirection * binWidth)) + middleBin);
		return GFTUtils.minMax(bin, 0, bins - 1);
	}
 
	private int mostVisitedBin() {
		int mostVisited = middleBin;
		for (int i = 0; i < bins; i++) {
			if (buffer[i] > buffer[mostVisited]) {
				mostVisited = i;
			}
		}
		return mostVisited;
	}	
}
 
class GFTUtils {
	static double bulletVelocity(double power) {
		return 20 - 3 * power;
	}
 
	static Point2D project(Point2D sourceLocation, double angle, double length) {
		return new Point2D.Double(sourceLocation.getX() + Math.sin(angle) * length,
				sourceLocation.getY() + Math.cos(angle) * length);
	}
 
	static double absoluteBearing(Point2D source, Point2D target) {
		return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
	}
 
	static int sign(double v) {
		return v < 0 ? -1 : 1;
	}
 
	static int minMax(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}
}
 
class GFTMovement {
	private static final double battlefieldW = 800;
	private static final double battlefieldH = 600;
	private static final double wallMarg = 18;
	private static final double maxT = 125;
	private static final double revT = 0.421075;
	private static final double defalt = 1.2;
	private static final double wallB = 0.699484;
 
	private AdvancedRobot robot;
	private Rectangle2D fieldRectangle = new Rectangle2D.Double(wallMarg, wallMarg,
		battlefieldW - wallMarg * 2, battlefieldH - wallMarg * 2);
	private double enemyFirePower = 3;
	private double direction = 0.4;
 
	GFTMovement(AdvancedRobot _robot) {
		this.robot = _robot;
	}
 
	public void onScannedRobot(ScannedRobotEvent e) {
		double enemyAbsoluteBearing = robot.getHeadingRadians() + e.getBearingRadians();
		double enemyDistance = e.getDistance();
		Point2D robotLocation = new Point2D.Double(robot.getX(), robot.getY());
		Point2D enemyLocation = GFTUtils.project(robotLocation, enemyAbsoluteBearing, enemyDistance);
		Point2D robotDestination;
		double tries = 0;
		while (!fieldRectangle.contains(robotDestination = GFTUtils.project(enemyLocation, enemyAbsoluteBearing + Math.PI + direction,
				enemyDistance * (defalt - tries / 100.0))) && tries < maxT) {
			tries++;
		}
		if ((Math.random() < (GFTUtils.bulletVelocity(enemyFirePower) / revT) / enemyDistance ||
				tries > (enemyDistance / GFTUtils.bulletVelocity(enemyFirePower) / wallB))) {
			direction = -direction;
		}
		// Jamougha's cool way
		double angle = GFTUtils.absoluteBearing(robotLocation, robotDestination) - robot.getHeadingRadians();
		robot.setAhead(Math.cos(angle) * 100);
		robot.setTurnRightRadians(Math.tan(angle));
	}
}
