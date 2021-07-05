/*
   Copyright (c) 2017 Ahome' Innovation Technologies. All rights reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.ait.lienzo.client.core.shape;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.ait.lienzo.client.core.Attribute;
import com.ait.lienzo.client.core.Context2D;
import com.ait.lienzo.client.core.shape.wires.IControlHandle;
import com.ait.lienzo.client.core.shape.wires.IControlHandleList;
import com.ait.lienzo.client.core.types.BoundingBox;
import com.ait.lienzo.client.core.types.PathPartList;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.lienzo.client.core.types.Point2DArray;
import com.ait.lienzo.client.core.util.Geometry;
import com.ait.lienzo.shared.core.types.Direction;
import com.ait.lienzo.shared.core.types.ShapeType;
import com.ait.lienzo.tools.client.collection.NFastDoubleArray;
import elemental2.dom.DomGlobal;
import jsinterop.annotations.JsProperty;

import static com.ait.lienzo.shared.core.types.Direction.EAST;
import static com.ait.lienzo.shared.core.types.Direction.NONE;
import static com.ait.lienzo.shared.core.types.Direction.NORTH;
import static com.ait.lienzo.shared.core.types.Direction.NORTH_WEST;
import static com.ait.lienzo.shared.core.types.Direction.SOUTH;
import static com.ait.lienzo.shared.core.types.Direction.SOUTH_WEST;
import static com.ait.lienzo.shared.core.types.Direction.WEST;

public class OrthogonalMultipointPolyLine extends AbstractDirectionalMultiPointShape<OrthogonalMultipointPolyLine> {

    private Point2D m_headOffsetPoint;

    private Point2D m_tailOffsetPoint;

    private Point2DArray m_computedPoint2DArray;

    private double m_breakDistance;

    @JsProperty
    private double cornerRadius;

    public OrthogonalMultipointPolyLine(final Point2D... points) {
        this(Point2DArray.fromArrayOfPoint2D(points));
    }

    public OrthogonalMultipointPolyLine(final Point2DArray points) {
        super(ShapeType.ORTHOGONAL_POLYLINE);

        setControlPoints(points);
    }

    public OrthogonalMultipointPolyLine(final Point2DArray points, final double corner) {
        this(points);

        setCornerRadius(corner);
    }

    @Override
    public Map<IControlHandle.ControlHandleType, IControlHandleList> getControlHandles(List<IControlHandle.ControlHandleType> types) {
        return super.getControlHandles(types);
    }


    @Override
    public boolean parse() {
        return parseOLD();
        // return parseNEW();
    }

    public boolean parseNEW() {
        Point2DArray points = getControlPoints();
        m_headOffsetPoint = points.get(0);
        m_tailOffsetPoint = points.get(points.size() - 1);
        // drawPoints(points);
        drawOrthoLine(points);
        return true;
    }

    private void drawOrthoLine(Point2DArray points) {
        NFastDoubleArray p = drawOrthogonalLinePoints(points, getHeadDirection(), getTailDirection(), 5, getHeadOffset(), getTailOffset(), true);
        Point2DArray array = Point2DArray.fromNFastDoubleArray(p);
        drawPoints(array);
    }
    private void drawPoints(Point2DArray points) {
        getPathPartList().M(m_headOffsetPoint.getX(), m_headOffsetPoint.getY());
        final int size = points.size();
        for (int i = 0; i < size - 1; i++) {
            Point2D p0 = points.get(i);
            Point2D p1 = points.get(i + 1);
            getPathPartList().M(p0.getX(), p0.getY());
            getPathPartList().L(p1.getX(), p1.getY());
        }
    }

    public Point2DArray inferOrthogonalSegments() {
        Point2DArray result = new Point2DArray();
        result.push(points.get(0));
        for (int i = 0; i < points.size() - 1; i++) {
            Point2D p0 = points.get(i);
            Point2D p1 = points.get(i + 1);
            boolean vertical = isVertical(p0, p1);
            boolean horizontal = isHorizontal(p0, p1);
            if (!vertical && !horizontal) {
                Point2DArray ps = new Point2DArray();
                ps.push(p0);
                ps.push(p1);
                NFastDoubleArray p = drawOrthogonalLinePoints(ps, getHeadDirection(), getTailDirection(), 0, 0, 0, true);
                Point2DArray array = Point2DArray.fromNFastDoubleArray(p);
                array = OrthogonalMultipointPolyLine.correctComputedPoints(array);
                if (array.size() > 2) {
                    for (int j = 1; j < array.size() - 1; j++) {
                        Point2D op = array.get(j);
                        result.push(op);
                    }
                }
            }
            result.push(p1);
        }
        // DomGlobal.console.log("INFERRED == " + result);
        return result;
    }

    public void update(Point2D point, double x, double y) {
        double dx = x - point.getX();
        double dy = y - point.getY();
        if (dx == 0 && dy == 0) {
            return;
        }

        int index = getPointIndex(point);

        // Propagate backwards.
        propagate(index, i -> i - 1, dx, dy);

        // Propagate forward.
        propagate(index, i -> i + 1, dx, dy);

        point.setX(x);
        point.setY(y);

        if (points.size() == 2) {
            Point2DArray inferred = inferOrthogonalSegments();
            setPoints(inferred);
        } else {
            Point2DArray array = OrthogonalMultipointPolyLine.correctComputedPoints(getPoints());
            setPoints(array);
        }

        refresh();
        batch();
    }

    public void propagate(int index, Function<Integer, Integer> nextIndexSupplier, double dx, double dy) {
        int nextIndex = nextIndexSupplier.apply(index);
        // Don't propagate to fist & last points as well.
        if (nextIndex < 1 || nextIndex >= (points.size() -1)) {
            return;
        }

        Point2D ref = points.get(index);
        Point2D candidate = points.get(nextIndex);
        double tx = 0;
        double ty = 0;
        double rx = ref.getX() + dx;
        double ry = ref.getY() + dy;
        double offset = 0;
        int i = nextIndexSupplier.apply(nextIndex);
        boolean canForward = i > 0 && (i < points.size() - 1);
        if (isHorizontal(ref, candidate)) {
            ty = dy;
            boolean forward = ref.getX() - candidate.getX() > 0;
            double d = forward ? rx - candidate.getX() : candidate.getX() - rx;
            if (canForward && d < offset) {
                tx = forward ? d  - offset : -d + offset;
            }
        }
        if (isVertical(ref, candidate)) {
            tx = dx;
            boolean forward = ref.getY() - candidate.getY() > 0;
            double d = forward ? ry - candidate.getY() : candidate.getY() - ry;
            if (canForward && d < offset) {
                ty = forward ? d - offset: -d + offset;
            }
        }

        if (Math.abs(tx) > 0 || Math.abs(ty) > 0) {
            propagate(nextIndex, nextIndexSupplier, tx, ty);
        }

        candidate.setX(candidate.getX() + tx);
        candidate.setY(candidate.getY() + ty);

    }

    private int getPointIndex(Point2D point) {
        for (int i = 0; i < points.size(); i++) {
            Point2D candidate = points.get(i);
            if (point == candidate) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isVertical(Point2D p0, Point2D p1) {
        return p1.getX() == p0.getX();
    }

    private static boolean isHorizontal(Point2D p0, Point2D p1) {
        return p1.getY() == p0.getY();
    }

    public Point2DArray getComputedPoint2DArray() {
        return m_computedPoint2DArray;
    }

    public static Point2DArray correctComputedPoints(Point2DArray points) {
        Point2DArray result = new Point2DArray();
        if (points.size() == 2) {
            result.push(points.get(0));
            result.push(points.get(1));
        } else if (points.size() > 2) {
            Point2D ref = points.get(0);
            result.push(ref);
            for (int i = 1; i < (points.size() -1); i++) {
                Point2D p0 = points.get(i);
                Point2D p1 = points.get(i + 1);

                boolean write = true;
                if (ref.getX() == p0.getX() && p0.getX() == p1.getX()) {
                    write = false;
                }
                if (ref.getY() == p0.getY() && p0.getY() == p1.getY()) {
                    write = false;
                }

                if (write) {
                    result.push(p0);
                }

                if (i == points.size() - 2) {
                    result.push(p1);
                }

                ref = p0;

            }
        }
        return result;
    }

















    public boolean parseOLD() {
        Point2DArray points = getControlPoints();
        return parseOLD(points);
    }

    public boolean parseOLD(Point2DArray p) {
        Point2DArray points = correctBreakDistance(p, m_breakDistance);

        if (points.size() > 1) {
            final double headOffset = getHeadOffset();
            final double correction = getCorrectionOffset();
            Direction headDirection = getHeadDirection();
            Direction tailDirection = getTailDirection();

            if (headDirection == NONE) {
                Point2D p0 = points.get(0);
                Point2D p1 = points.get(1);
                double headOffsetAndCorrect = headOffset + correction;
                headDirection = getHeadDirection(headDirection, p0, p1, headOffsetAndCorrect);
            }

            final NFastDoubleArray opoint = drawOrthogonalLinePoints(points, headDirection, tailDirection, correction, this.getHeadOffset(), this.getTailOffset(), true);

            m_headOffsetPoint = points.get(0);
            m_tailOffsetPoint = points.get(points.size() - 1);

            if (null != opoint) {
                final PathPartList list = getPathPartList();
                list.M(m_headOffsetPoint.getX(), m_headOffsetPoint.getY());
                final double radius = getCornerRadius();

                m_computedPoint2DArray = Point2DArray.fromNFastDoubleArray(opoint);

                if (radius > 0) {
                    Geometry.drawArcJoinedLines(list, m_computedPoint2DArray, radius);
                } else {
                    final int size = opoint.size();
                    // start at 2, as M is for opoint[0]
                    for (int i = 2; i < size; i += 2) {
                        list.L(opoint.get(i), opoint.get(i + 1));
                    }
                }
            }

            return true;
        }

        m_computedPoint2DArray = null;
        return false;
    }

    public final Point2DArray correctBreakDistance(Point2DArray points, double breakDistance) {
        Point2DArray cPoints = points.copy();

        Point2D p1, p2;

        final int size = cPoints.size();

        for (int i = 0; i < size - 1; i++) {
            p1 = cPoints.get(i);
            p2 = cPoints.get(i + 1);

            if (Geometry.closeEnough(p1.getX(), p2.getX(), breakDistance)) {
                p2.setX(p1.getX());
            }

            if (Geometry.closeEnough(p1.getY(), p2.getY(), breakDistance)) {
                p2.setY(p1.getY());
            }
        }

        return cPoints;
    }

    private static final Direction getHeadDirection(Direction headDirection, Point2D p0, Point2D p1, double headOffsetAndCorrection) {
        double p0x = p0.getX();
        double p0y = p0.getY();
        double p1x = p1.getX();
        double p1y = p1.getY();
        final double dx = (p1x - p0x);
        final double dy = (p1y - p0y);

        boolean verticalOverlap = (dx > -headOffsetAndCorrection && dx < headOffsetAndCorrection);
        boolean horizontalOverlap = (dy > -headOffsetAndCorrection && dy < headOffsetAndCorrection);

        switch (headDirection) {
            case NONE: {
                Direction p0ToP1Direction = Geometry.getQuadrant(p0x, p0y, p1x, p1y);
                switch (p0ToP1Direction) {
                    case SOUTH_WEST:
                    case SOUTH_EAST:
                        headDirection = (p0ToP1Direction == SOUTH_WEST) ? WEST : EAST;
                        if (verticalOverlap) {
                            headDirection = SOUTH;
                        }
                        break;
                    case NORTH_WEST:
                    case NORTH_EAST:
                        headDirection = (p0ToP1Direction == NORTH_WEST) ? WEST : EAST;
                        if (!horizontalOverlap) {
                            headDirection = NORTH;
                        }
                        break;
                }
                break;
            }
            default:
                // return head and tail, as is.
                break;
        }

        return headDirection;
    }

    public static final Point2D correctEndWithOffset(double offset, Direction direction, final Point2D target) {
        switch (direction) {
            case NORTH:
                return target.setY(target.getY() - offset);
            case EAST:
                return target.setX(target.getX() + offset);
            case SOUTH:
                return target.setY(target.getY() + offset);
            case WEST:
                return target.setX(target.getX() - offset);
            case NONE:
                return target;
            default:
                throw new IllegalStateException("Invalid Direction " + direction);
        }
    }

    private static Point2D correctP0(Direction headDirection, double correction, final double headOffset, boolean write, NFastDoubleArray buffer, Point2D p0) {
        if (!write) {
            p0 = p0.copy();
        }

        // correct for headOffset
        if (headOffset > 0) {
            correctEndWithOffset(headOffset, headDirection, p0);
        }

        // addBoundingBox starting point, that may have head offset
        addPoint(buffer, p0.getX(), p0.getY(), write);

        // correct for correction
        if (correction > 0) {
            // must do this off a cloned Point2D, as points[0] is used for M operation, during line drawing.
            if (write) {
                // if !write, we are already working on a copy
                p0 = p0.copy();
            }
            correctEndWithOffset(correction, headDirection, p0);
            // addBoundingBox another point of the correction, to ensure the line is always visible at the tip of the arrow
            addPoint(buffer, p0.getX(), p0.getY(), write);
        }
        return p0;
    }

    private static final NFastDoubleArray drawOrthogonalLinePoints(final Point2DArray points, Direction headDirection, Direction tailDirection, final double correction, double headOffset, double tailOffset, boolean write) {
        final NFastDoubleArray buffer = new NFastDoubleArray();

        Point2D p0 = points.get(0);
        p0 = correctP0(headDirection, correction, headOffset, write, buffer, p0);

        int i = 1;
        Direction direction = headDirection;
        final int size = points.size();
        Point2D p1;

        for (; i < size - 1; i++) {
            p1 = points.get(i);

            if (points.size() > 2 && i > 1) {
                direction = getNextDirection(direction, p0.getX(), p0.getY(), p1.getX(), p1.getY());
                addPoint(buffer, p1.getX(), p1.getY(), write);
            } else {
                direction = drawOrthogonalLineSegment(buffer, direction, null, p0.getX(), p0.getY(), p1.getX(), p1.getY(), write);
            }

            if (null == direction) {
                return null;
            }
            p0 = p1;
        }
        p1 = points.get(size - 1);

        if (points.size() == 2 || (points.size() > 2 && (isVertical(p0, p1) || isHorizontal(p0, p1)))) {
            drawTail(points, buffer, direction, tailDirection, p0, p1, correction, headOffset, tailOffset);
        } else {
            addPoint(buffer, p1.getX(), p1.getY(), write);
        }

        return buffer;
    }

    /**
     * Draws an orthogonal line between two points, it uses the previous direction to determine the new direction. It
     * will always attempt to continue the line in the same direction if it can do so, without requiring a corner.
     * If the line goes back on itself, it'll go 50% of the way  and then go perpendicular, so that it no longer goes back on itself.
     */
    private static final Direction drawOrthogonalLineSegment(final NFastDoubleArray buffer, final Direction direction, Direction nextDirection, double p1x, double p1y, final double p2x, final double p2y, boolean write) {
        if (nextDirection == null) {
            nextDirection = getNextDirection(direction, p1x, p1y, p2x, p2y);
        }

        if ((nextDirection == SOUTH) || (nextDirection == NORTH)) {
            if (p1x == p2x) {
                // points are already on a straight line, so don't try and apply an orthogonal line
                addPoint(buffer, p2x, p2y, write);
            } else {
                addPoint(buffer, p1x, p2y, p2x, p2y, write);
            }
            if (p1x < p2x) {
                return EAST;
            } else if (p1x > p2x) {
                return WEST;
            } else {
                return nextDirection;
            }
        } else {
            if (p1y != p2y) {
                addPoint(buffer, p2x, p1y, p2x, p2y, write);
            } else {
                // points are already on a straight line, so don't try and apply an orthogonal line
                addPoint(buffer, p2x, p2y, write);
            }
            if (p1y > p2y) {
                return NORTH;
            } else if (p1y < p2y) {
                return SOUTH;
            } else {
                return nextDirection;
            }
        }
    }

    /**
     * looks at the current and target points and based on the current direction returns the next direction. This drives the orthogonal line drawing.
     *
     * @param direction
     * @param p1x
     * @param p1y
     * @param p2x
     * @param p2y
     * @return
     */
    private static Direction getNextDirection(Direction direction, double p1x, double p1y, double p2x, double p2y) {
        Direction next_direction;

        switch (direction) {
            case NORTH:
                if (p2y < p1y) {
                    next_direction = NORTH;
                } else if (p2x > p1x) {
                    next_direction = EAST;
                } else {
                    next_direction = WEST;
                }
                break;
            case SOUTH:
                if (p2y > p1y) {
                    next_direction = SOUTH;
                } else if (p2x > p1x) {
                    next_direction = EAST;
                } else {
                    next_direction = WEST;
                }
                break;
            case EAST:
                if (p2x > p1x) {
                    next_direction = EAST;
                } else if (p2y < p1y) {
                    next_direction = NORTH;
                } else {
                    next_direction = SOUTH;
                }
                break;
            case WEST:
                if (p2x < p1x) {
                    next_direction = WEST;
                } else if (p2y < p1y) {
                    next_direction = NORTH;
                } else {
                    next_direction = SOUTH;
                }
                break;
            default:
                throw new IllegalStateException("This should not be reached (Defensive Code)");
        }
        return next_direction;
    }

    /**
     * When tail is NONE it needs to try multiple directions to determine which gives the least number of corners, and then selects that as the final direction.
     */
    private static Direction getTailDirection(Point2DArray points, NFastDoubleArray buffer, Direction lastDirection, Direction tailDirection, double correction, double headOffset, double tailOffset, double p0x, double p0y, double p1x, double p1y) {
        double offset = headOffset + correction;
        switch (tailDirection) {
            case NONE: {
                final double dx = (p1x - p0x);
                final double dy = (p1y - p0y);

                int bestPoints = 0;

                if (dx > offset) {
                    tailDirection = WEST;
                    bestPoints = drawTail(points, buffer, lastDirection, WEST, correction, tailOffset, p0x, p0y, p1x, p1y, false);
                } else {
                    tailDirection = EAST;
                    bestPoints = drawTail(points, buffer, lastDirection, EAST, correction, tailOffset, p0x, p0y, p1x, p1y, false);
                }

                if (dy > 0) {
                    int points3 = drawTail(points, buffer, lastDirection, NORTH, correction, tailOffset, p0x, p0y, p1x, p1y, false);

                    if (points3 < bestPoints) {
                        tailDirection = NORTH;
                        bestPoints = points3;
                    }
                } else {
                    int points4 = drawTail(points, buffer, lastDirection, SOUTH, correction, tailOffset, p0x, p0y, p1x, p1y, false);
                    if (points4 < bestPoints) {
                        tailDirection = SOUTH;
                        bestPoints = points4;
                    }
                }

                break;
            }
            default:
                break;
        }
        return tailDirection;
    }

    private static final void drawTail(Point2DArray points, NFastDoubleArray buffer, Direction lastDirection, Direction tailDirection, Point2D p0, Point2D p1, final double correction, final double headoffset, final double tailOffset) {
        double p0x = p0.getX();

        double p0y = p0.getY();

        double p1x = p1.getX();

        double p1y = p1.getY();

        // This returns an array, as drawTail needs both the direction and the number of corner points.
        tailDirection = getTailDirection(points, buffer, lastDirection, tailDirection, correction, headoffset, tailOffset, p0x, p0y, p1x, p1y);

        drawTail(points, buffer, lastDirection, tailDirection, correction, tailOffset, p0x, p0y, p1x, p1y, true);
    }

    /**
     * Draws the last segment of the line to the tail.
     * It will take into account the correction and arrow.
     * Logic is applied to help draw an attractive line. Under certain conditions it will attempt to addBoundingBox an extra mid point. For example if you have directions
     * going opposite to each other, it will create a mid point so that the line goes back on itseld through this mid point.
     */
    private static int drawTail(Point2DArray points, NFastDoubleArray buffer, Direction lastDirection, Direction tailDirection, double correction, double tailOffset, double p0x, double p0y, double p1x, double p1y, boolean write) {
        Point2D p1 = points.get(points.size() - 1);

        // correct for tailOffset
        if (tailOffset > 0) {
            if (!write) {
                p1 = p1.copy();
            }
            correctEndWithOffset(tailOffset, tailDirection, p1);
            p1x = p1.getX();
            p1y = p1.getY();
        }

        // correct for correction
        if (correction > 0) {
            // must do this off a cloned Point2D, as we still need the p1, for the last part of the line at the end.
            Point2D p1Copy = p1.copy();
            correctEndWithOffset(correction, tailDirection, p1Copy);
            p1x = p1Copy.getX();
            p1y = p1Copy.getY();
        }

        final double dx = (p1x - p0x);
        final double dy = (p1y - p0y);

        int corners = 0;

        boolean behind = false;

        switch (tailDirection) {
            case NORTH:
                behind = dy < 0;
                break;
            case SOUTH:
                behind = dy > 0;
                break;
            case WEST:
                behind = dx < 0;
                break;
            case EAST:
                behind = dx > 0;
                break;
            case NONE:
                // do nothing as NONE is explicitey handled at the end
                break;
            default:
                throw new IllegalStateException("Invalid Direction " + tailDirection);
        }
        double x = p0x;

        double y = p0y;

        if (behind) {
            // means p0 is behind.
            switch (tailDirection) {
                case NORTH:
                case SOUTH:
                    if ((lastDirection == NORTH && tailDirection == SOUTH) ||
                            (lastDirection == SOUTH && tailDirection == NORTH) ||
                            (dx > 0 && lastDirection == EAST) ||
                            (dx < 0 && lastDirection == WEST)) {
                        // A mid point is needed to ensure an attractive line is drawn.
                        x = p0x + (dx / 2);
                        addPoint(buffer, x, y, write);

                        if (lastDirection == NORTH || lastDirection == SOUTH) {
                            corners++;
                        }
                    }

                    y = p1y;
                    addPoint(buffer, x, y, write);
                    if (lastDirection != tailDirection) {
                        corners++;
                    }

                    x = p1x;
                    addPoint(buffer, x, y, write);
                    corners++;

                    y = p1.getY();
                    addPoint(buffer, x, y, write);
                    corners++;
                    break;
                case WEST:
                case EAST:
                    if ((lastDirection == WEST && tailDirection == EAST) ||
                            (lastDirection == EAST && tailDirection == WEST) ||
                            (dy > 0 && lastDirection == SOUTH) ||
                            (dy < 0 && lastDirection == NORTH)) {
                        // A mid point is needed to ensure an attrictive line is drawn.
                        y = p0y + (dy / 2);
                        addPoint(buffer, x, y, write);

                        if (lastDirection == EAST || lastDirection == WEST) {
                            corners++;
                        }
                    }

                    x = p1x;
                    addPoint(buffer, x, y, write);
                    if (lastDirection != tailDirection) {
                        corners++;
                    }

                    y = p1y;
                    addPoint(buffer, x, y, write);
                    corners++;

                    x = p1.getX();
                    addPoint(buffer, x, y, write);
                    corners++;
                    break;
                default:
                    throw new IllegalStateException("Invalid Direction " + tailDirection);
            }
        } else {
            // means p0 is in front
            switch (tailDirection) {
                case NORTH:
                case SOUTH:
                    if ((lastDirection == NORTH && tailDirection == SOUTH) ||
                            (lastDirection == SOUTH && tailDirection == NORTH) ||
                            (dx > 0 && lastDirection == WEST) ||
                            (dx < 0 && lastDirection == EAST)) {
                        // A mid point is needed to ensure an attrictive line is drawn.
                        y = p0y + (dy / 2);
                        addPoint(buffer, x, y, write);

                        if (lastDirection == EAST || lastDirection == WEST) {
                            lastDirection = (dy < 0) ? NORTH : SOUTH;
                            corners++;
                        }
                    }

                    x = p1x;
                    addPoint(buffer, x, y, write);
                    if (lastDirection == NORTH || lastDirection == SOUTH) {
                        corners++;
                    }

                    y = p1.getY();
                    addPoint(buffer, x, y, write);
                    corners++;
                    break;
                case WEST:
                case EAST:
                    if ((lastDirection == WEST && tailDirection == EAST) ||
                            (lastDirection == EAST && tailDirection == WEST) ||
                            (dy > 0 && lastDirection == NORTH) ||
                            (dy < 0 && lastDirection == SOUTH)) {
                        // A mid point is needed to ensure an attrictive line is drawn.
                        x = p0x + (dx / 2);
                        addPoint(buffer, x, y, write);

                        if (lastDirection == NORTH || lastDirection == SOUTH) {
                            lastDirection = (dx < 0) ? WEST : EAST;
                            corners++;
                        }
                    }

                    y = p1y;
                    addPoint(buffer, x, y, write);
                    if (lastDirection == EAST || lastDirection == WEST) {
                        corners++;
                    }

                    x = p1.getX();
                    addPoint(buffer, x, y, write);
                    corners++;
                    break;
                default:
                    throw new IllegalStateException("Invalid Direction " + tailDirection);
            }
        }

        return corners;
    }

    private static final void addPoint(final NFastDoubleArray buffer, final double x, final double y, boolean write) {
        if (write == true) {
            addPoint(buffer, x, y);
        }
    }

    private static final void addPoint(final NFastDoubleArray buffer, final double x0, final double y0, double x1, double y1, boolean write) {
        if (write == true) {
            buffer.push(x0, y0, x1, y1);
        }
    }

    private static final void addPoint(final NFastDoubleArray buffer, final double x, final double y) {
        // always attempt to normalise
        if (!buffer.isEmpty()) {
            double x1 = buffer.get(buffer.size() - 2);
            double y1 = buffer.get(buffer.size() - 1);

            if (x == x1 && y == y1) {
                // New point is the same as old point. The code should probably be changed, so that situation didn't occur.
                // But at the moment not entirely sure how to do that, so fixing sympton that than cause (mdp).
                return;
            }
        }

        buffer.push(x, y);
    }

    @Override
    public BoundingBox getBoundingBox() {
        if (getPathPartList().size() < 1) {
            if (false == parse()) {
                return BoundingBox.fromDoubles(0, 0, 0, 0);
            }
        }
        return getPathPartList().getBoundingBox();
    }

    @Override
    protected boolean fill(Context2D context, double alpha) {
        return false;
    }

    public double getCornerRadius() {
        return this.cornerRadius;
    }

    public OrthogonalMultipointPolyLine setCornerRadius(final double radius) {
        this.cornerRadius = cornerRadius;

        return refresh();
    }

    public double getBreakDistance() {
        return m_breakDistance;
    }

    public OrthogonalMultipointPolyLine setBreakDistance(double distance) {
        m_breakDistance = distance;

        return refresh();
    }

    @Override
    public OrthogonalMultipointPolyLine setPoint2DArray(final Point2DArray points) {
        return setControlPoints(points);
    }

    @Override
    public Point2DArray getPoint2DArray() {
        return getControlPoints();
    }

    @Override
    public boolean isControlPointShape() {
        return true;
    }

    @Override
    public Point2D getHeadOffsetPoint() {
        return m_headOffsetPoint;
    }

    @Override
    public Point2D getTailOffsetPoint() {
        return m_tailOffsetPoint;
    }

    @Override
    public List<Attribute> getBoundingBoxAttributes() {
        return getBoundingBoxAttributesComposed(Attribute.CONTROL_POINTS, Attribute.CORNER_RADIUS);
    }

    @Override
    public Shape<OrthogonalMultipointPolyLine> copyTo(Shape<OrthogonalMultipointPolyLine> other) {
        super.copyTo(other);
        ((OrthogonalMultipointPolyLine) other).m_headOffsetPoint = m_headOffsetPoint.copy();
        ((OrthogonalMultipointPolyLine) other).m_tailOffsetPoint = m_tailOffsetPoint.copy();
        ((OrthogonalMultipointPolyLine) other).m_computedPoint2DArray = m_computedPoint2DArray.copy();
        ((OrthogonalMultipointPolyLine) other).m_breakDistance = m_breakDistance;
        ((OrthogonalMultipointPolyLine) other).cornerRadius = cornerRadius;

        return other;
    }

    @Override
    public OrthogonalMultipointPolyLine cloneLine() {
        OrthogonalMultipointPolyLine orthogonalPolyLine = new OrthogonalMultipointPolyLine(this.getControlPoints().copy(), cornerRadius);
        return (OrthogonalMultipointPolyLine) copyTo(orthogonalPolyLine);
    }
}