package org.jhotdraw8.geom.offsetline;

import javafx.geometry.Point2D;
import org.jhotdraw8.geom.Geom;

import java.util.List;

public class Utils {
    /**
     * absolute threshold to be used for reals in common geometric computation (e.g. to check for
     * singularities).
     */
    public final static double realPrecision = 1e-5;
    /**
     * absolute threshold to be used for joining slices together at end points
     */
    public final static double sliceJoinThreshold = 1e-4;
    /**
     * absolute threshold to be used for comparing reals generally.
     */
    public final static double realThreshold = 1e-8;

    public static final double tau = 2.0 * Math.PI;

    public static boolean fuzzyEqual(Point2D v1, Point2D v2) {
        return fuzzyEqual(v1, v2, Utils.realThreshold);
    }

    public static boolean fuzzyEqual(Point2D v1, Point2D v2, double epsilon) {
        return Geom.squaredDistance(v1, v2) < epsilon * epsilon;
    }

    public static boolean fuzzyEqual(double x, double y) {
        return fuzzyEqual(x, y, realThreshold);
    }

    public static boolean fuzzyEqual(double x, double y, double epsilon) {
        return Math.abs(x - y) < epsilon;
    }

    /// Perpendicular dot product. Equivalent to dot(v0, perp(v1)).
    public static double perpDot(final Point2D v0, final Point2D v1) {
        return v0.getX() * v1.getY() - v0.getY() * v1.getX();
    }

    public static MinMax minmax(double a, double b) {
        return new MinMax(b, a);
    }

    public static boolean fuzzyInRange(double minValue, double value, double maxValue) {
        return fuzzyInRange(minValue, value, maxValue, realThreshold);
    }

    public static boolean fuzzyInRange(double minValue, double value, double maxValue, double epsilon) {
        return (value + epsilon > minValue) && (value < maxValue + epsilon);
    }

    /**
     * Test if a point is within a arc sweep angle region defined by center, start, end, and bulge.
     */
    static boolean pointWithinArcSweepAngle(final Point2D center, final Point2D arcStart,
                                            final Point2D arcEnd, double bulge, final Point2D point) {
        assert Math.abs(bulge) > Utils.realThreshold : "expected arc";
        assert Math.abs(bulge) <= 1.0 : "bulge should always be between -1 and 1";

        if (bulge > 0.0) {
            return isLeftOrCoincident(center, arcStart, point) &&
                    isRightOrCoincident(center, arcEnd, point);
        }

        return isRightOrCoincident(center, arcStart, point) && isLeftOrCoincident(center, arcEnd, point);
    }

    /**
     * Returns true if point is left or fuzzy coincident with the line pointing in the direction of the
     * vector (p1 - p0).
     */
    static boolean isLeftOrCoincident(final Point2D p0, final Point2D p1,
                                      final Point2D point) {
        return isLeftOrCoincident(p0, p1, point, realThreshold);
    }

    static boolean isLeftOrCoincident(final Point2D p0, final Point2D p1,
                                      final Point2D point, double epsilon) {
        return (p1.getX() - p0.getX()) * (point.getY() - p0.getY()) - (p1.getY() - p0.getY()) * (point.getX() - p0.getX()) >
                -epsilon;
    }

    /**
     * Returns true if point is right or fuzzy coincident with the line pointing in the direction of
     * the vector (p1 - p0).
     */
    static boolean isRightOrCoincident(final Point2D p0, final Point2D p1,
                                       final Point2D point) {
        return isRightOrCoincident(p0, p1, point, realThreshold);
    }

    static boolean isRightOrCoincident(final Point2D p0, final Point2D p1,
                                       final Point2D point, double epsilon) {
        return (p1.getX() - p0.getX()) * (point.getY() - p0.getY()) - (p1.getY() - p0.getY()) * (point.getX() - p0.getX()) <
                epsilon;
    }

    /**
     * Returns the solutions to for the quadratic equation -b +/- sqrt (b * b - 4 * a * c) / (2 * a).
     */
    static MinMax quadraticSolutions(double a, double b, double c, double discr) {
        // Function avoids loss in precision due to taking the difference of two floating point values
        // that are very near each other in value.
        // See:
        // https://math.stackexchange.com/questions/311382/solving-a-quadratic-equation-with-precision-when-using-floating-point-variables
        assert fuzzyEqual(b * b - 4.0 * a * c, discr) : "discriminate is not correct";
        double sqrtDiscr = Math.sqrt(discr);
        double denom = 2.0 * a;
        double sol1;
        if (b < 0.0) {
            sol1 = (-b + sqrtDiscr) / denom;
        } else {
            sol1 = (-b - sqrtDiscr) / denom;
        }

        double sol2 = (c / a) / sol1;

        return new MinMax(sol1, sol2);
    }

    /**
     * Return the point on the segment going from p0 to p1 at parametric value t.
     */
    public static Point2D pointFromParametric(final Point2D p0, final Point2D p1, double t) {
        return p0.add(p1.subtract(p0).multiply(t));
    }

    /**
     * Counter clockwise angle of the vector going from p0 to p1.
     */
    public static double angle(final Point2D p0, final Point2D p1) {
        return Math.atan2(p1.getY() - p0.getY(), p1.getX() - p0.getX());
    }

    /**
     * Returns the smaller difference between two angles, result is negative if angle2 < angle1.
     */
    public static double deltaAngle(double angle1, double angle2) {
        double diff = normalizeRadians(angle2 - angle1);
        if (diff > Math.PI) {
            diff -= tau;
        }

        return diff;
    }

    /**
     * Normalize radius to be between 0 and 2PI, e.g. -PI/4 becomes 7PI/8 and 5PI becomes PI.
     */
    public static double normalizeRadians(double angle) {
        if (angle >= 0.0 && angle <= tau) {
            return angle;
        }

        return angle - Math.floor(angle / tau) * tau;
    }

    /**
     * Normalized perpendicular vector to v (rotating counter clockwise).
     */
    public static Point2D unitPerp(Point2D v) {
        Point2D result = new Point2D(-v.getY(), v.getX());
        return result.normalize();
    }

    static <T> int nextWrappingIndex(int index, List<T> container) {
        if (index == container.size() - 1) {
            return 0;
        }

        return index + 1;
    }

    static <T> int prevWrappingIndex(int index, List<T> container) {
        if (index == 0) {
            return container.size() - 1;
        }

        return index - 1;
    }

    static boolean angleIsWithinSweep(double startAngle, double sweepAngle, double testAngle) {
        return angleIsWithinSweep(startAngle, sweepAngle, testAngle, realThreshold);
    }

    static boolean angleIsWithinSweep(double startAngle, double sweepAngle, double testAngle,
                                      double epsilon) {
        double endAngle = startAngle + sweepAngle;
        if (sweepAngle < 0.0) {
            return angleIsBetween(endAngle, startAngle, testAngle, epsilon);
        }

        return angleIsBetween(startAngle, endAngle, testAngle, epsilon);
    }

    static boolean angleIsBetween(double startAngle, double endAngle, double testAngle) {
        return angleIsBetween(startAngle, endAngle, testAngle, realThreshold);
    }

    static boolean angleIsBetween(double startAngle, double endAngle, double testAngle,
                                  double epsilon) {
        double endSweep = normalizeRadians(endAngle - startAngle);
        double midSweep = normalizeRadians(testAngle - startAngle);

        return midSweep < endSweep + epsilon;
    }
}
