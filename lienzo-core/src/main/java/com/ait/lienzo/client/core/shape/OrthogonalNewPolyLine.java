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
import java.util.Collections;
import java.util.List;

import com.ait.lienzo.client.core.Attribute;
import com.ait.lienzo.client.core.Context2D;
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

public class OrthogonalNewPolyLine extends AbstractDirectionalMultiPointShape<OrthogonalNewPolyLine> {

    private Point2D m_headOffsetPoint;

    private Point2D m_tailOffsetPoint;

    private double m_breakDistance;

    @JsProperty
    private double cornerRadius;

    private Point2DArray m_computedPoint2DArray;

    public static OrthogonalNewPolyLine inferPoints(Point2DArray points) {
        if (true) {
            return new OrthogonalNewPolyLine(new Point2D(0, 0), new Point2D(100, 0));
        }
        Point2DArray inferred = inferOrthogonalSegments(points.get(0), points.get(points.size() - 1), Direction.NONE, Direction.NONE, 10, 10);
        DomGlobal.console.log("INFERRED [" + inferred + "]");
        return new OrthogonalNewPolyLine(inferred);
    }

    public OrthogonalNewPolyLine(final Point2D... points) {
        this(Point2DArray.fromArrayOfPoint2D(points));
    }

    public OrthogonalNewPolyLine(final Point2DArray points) {
        super(ShapeType.ORTHOGONAL_POLYLINE);

        setControlPoints(points);
        setHeadDirection(NONE);
        setTailDirection(NONE);
    }

    public OrthogonalNewPolyLine(final Point2DArray points, final double corner) {
        this(points);

        setCornerRadius(corner);
    }

    @Override
    public boolean parse() {
        if (0 == points.size()) {
            return false;
        }

        infer();

        inferDirectionChanges();

        if (parseAsPolyline()) {
            calculateNonOrthogonalPoints();
            return true;
        }

        return false;
    }

    public boolean parseAsPolyline() {
        Point2DArray list = points;

        list = list.noAdjacentPoints();
        final int size = list.size();

        if (0 == size) {
            return false;
        }

        final PathPartList path = getPathPartList();
        final double headOffset = getHeadOffset();
        final double tailOffset = getTailOffset();

        if (size > 1) {
            m_headOffsetPoint = Geometry.getProjection(list.get(0), list.get(1), headOffset);
            m_tailOffsetPoint = Geometry.getProjection(list.get(size - 1), list.get(size - 2), tailOffset);

            path.M(m_headOffsetPoint);

            final double corner = getCornerRadius();
            if (corner <= 0) {
                for (int i = 1; i < size - 1; i++) {
                    path.L(list.get(i));
                }

                path.L(m_tailOffsetPoint);
            } else {
                list = list.copy();
                list.set(size - 1, m_tailOffsetPoint);

                Geometry.drawArcJoinedLines(path, list, corner);
            }
        } else if (size == 1) {
            m_headOffsetPoint = list.get(0).copy().offset(headOffset, headOffset);
            m_tailOffsetPoint = list.get(0).copy().offset(tailOffset, tailOffset);

            path.M(m_headOffsetPoint);

            final double corner = getCornerRadius();
            if (corner <= 0) {
                path.L(m_tailOffsetPoint);
            } else {
                list = Point2DArray.fromArrayOfPoint2D(list.get(0).copy(), list.get(0).copy());

                Geometry.drawArcJoinedLines(path, list, corner);
            }
        }

        return true;
    }

    private boolean isHeadDirectionChanged() {
        int size = points.size();
        if (size > 2) {
            Direction headDirection = getHeadDirection();
            if (null != headDirection && !headDirection.equals(NONE)) {
                Point2D p0 = points.get(0);
                Point2D p1 = points.get(1);
                if (isOrthogonal(p0, p1) && getDefaultHeadOffset() != 0) {
                    Direction actualHeadDirection = getOrthogonalDirection(p0, p1);
                    if (!headDirection.equals(actualHeadDirection)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isTailDirectionChanged() {
        int size = points.size();
        if (size > 2) {
            Direction tailDirection = getTailDirection();
            if (null != tailDirection && !tailDirection.equals(NONE)) {
                Point2D pN_1 = points.get(size - 2);
                Point2D pN = points.get(size - 1);
                if (isOrthogonal(pN, pN_1) && getDefaultTailOffset() != 0) {
                    Direction actualTailDirection = getOrthogonalDirection(pN, pN_1);
                    if (!tailDirection.equals(actualTailDirection)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void inferDirectionChanges() {
        if (isHeadDirectionChanged()) {
            DomGlobal.console.log("HEAD DIRECTION CHANGED - REBUILDING POINTS!");
            resetHeadDirectionPoints();
        }
        if (isTailDirectionChanged()) {
            DomGlobal.console.log("TAIL DIRECTION CHANGED - REBUILDING POINTS!");
            resetTailDirectionPoints();
        }
    }

    private void resetHeadDirectionPoints() {
        int size = points.size();
        Point2D p0 = points.get(0);

        int i = 1;
        for (; i < size; i++) {
            Point2D pI = points.get(i);
            if (nonOrthogonalPoints.contains(pI)) {
                break;
            }
        }

        Point2D pI = points.get(i - 1);
        Point2DArray headPoints = inferOrthogonalSegments(p0, pI, getHeadDirection(), getTailDirection(), getDefaultHeadOffset(), getDefaultTailOffset());
        for (; i < size; i++) {
            headPoints.push(points.get(i));
        }
        this.points = headPoints;
    }

    private void resetTailDirectionPoints() {
        int size = points.size();
        Point2D p0 = points.get(size - 1);

        int i = size - 2;
        for (; i >= 0; i--) {
            Point2D pI = points.get(i);
            if (nonOrthogonalPoints.contains(pI)) {
                break;
            }
        }

        Point2D pI = points.get(i + 1);
        Point2DArray tailPoints = inferOrthogonalSegments(pI, p0, getHeadDirection(), getTailDirection(), getDefaultHeadOffset(), getDefaultTailOffset());
        for (; i >= 0; i--) {
            tailPoints.push(points.get(i));
        }
        this.points = tailPoints;
    }

    private void infer() {
        if (!upIndexesToRecalculate.isEmpty()) {
            Point2DArray inferred = inferOrthogonalSegments(getHeadDirection(), getTailDirection(), getDefaultHeadOffset(), getDefaultTailOffset());
            Point2DArray corrected = correctComputedPoints(inferred, nonOrthogonalPoints);
            setPoints(corrected);
            getLayer().batch();
            upIndexesToRecalculate.clear();
        }
    }

    public void clearNonOrthogonalPoints() {
        nonOrthogonalPoints.clear();
    }

    public void calculateNonOrthogonalPoints() {
        if (nonOrthogonalPoints.isEmpty()) {
            int size = points.size();
            if (size > 2) {
                for (int i = 1; i < size - 1; i++) {
                    Point2D lastP = points.get(i - 1);
                    Point2D p = points.get(i);
                    Point2D nextP = points.get(i + 1);
                    if (!isOrthogonal(lastP, p) && !isOrthogonal(p, nextP)) {
                        nonOrthogonalPoints.add(p);
                    }
                }
            }
        }
    }

    private static final double DEFAULT_OFFSET = 10;

    public double getDefaultHeadOffset() {
        return super.getHeadOffset() > 0 ? super.getHeadOffset() : DEFAULT_OFFSET;
    }

    public double getDefaultTailOffset() {
        return super.getTailOffset() > 0 ? super.getTailOffset() : DEFAULT_OFFSET;
    }

    @Override
    public OrthogonalNewPolyLine setHeadDirection(Direction direction) {
        // DomGlobal.console.log("SET HEAD DIRECTION = " + direction);
        // TODO: Just don't call refresh on parent -> return super.setHeadDirection(direction);
        headDirection = direction;
        return this;
    }

    @Override
    public OrthogonalNewPolyLine setTailDirection(Direction direction) {
        // DomGlobal.console.log("SET TAIL DIRECTION = " + direction);
        // TODO: Just don't call refresh on parent -> // return super.setTailDirection(direction);
        tailDirection = direction;
        return this;
    }

    @Override
    public OrthogonalNewPolyLine setHeadOffset(double offset) {
        // TODO: Just don't call refresh on parent -> return super.setHeadOffset(offset);
        this.headOffset = offset;
        return this;
    }

    @Override
    public OrthogonalNewPolyLine setTailOffset(double offset) {
        // TODO: Just don't call refresh on parent -> return super.setTailOffset(offset);
        this.tailOffset = offset;
        return this;
    }

    @Override
    public OrthogonalNewPolyLine setControlPoints(Point2DArray points) {
        // TODO: Just don't call refresh on parent -> return super.setControlPoints(points);
        this.points = points;
        return this;
    }

    @Override
    public OrthogonalNewPolyLine setPoints(Point2DArray points) {
        // TODO: Just don't call refresh on parent -> return super.setPoints(points);
        this.points = points;
        return this;
    }

    @Override
    public OrthogonalNewPolyLine refresh() {
        return super.refresh();
    }

    private static boolean isOrthogonal(Point2D p0, Point2D p1) {
        return isVertical(p0, p1) || isHorizontal(p0, p1);
    }

    public static Direction getOrthogonalDirection(final Point2D p0, final Point2D p1) {
        if (isHorizontal(p0, p1)) {
            return p0.getX() < p1.getX() ? EAST : WEST;
        }
        if (isVertical(p0, p1)) {
            return p0.getY() < p1.getY() ? SOUTH : NORTH;
        }
        return NONE;
    }


    // TODO: Refactor those booleans by the use of nonOrthogonalPoints?
    public boolean isFirstSegmentOrthogonal = true;
    public boolean isLastSegmentOrthogonal = true;
    private List<Point2D> nonOrthogonalPoints = new ArrayList<Point2D>();

    private List<Integer> upIndexesToRecalculate = new ArrayList<Integer>();

    public void update(int index, double x, double y) {

        // upIndexesToRecalculate.clear();

        Point2D point = points.get(index);
        double dx = x - point.getX();
        double dy = y - point.getY();
        if (dx == 0 && dy == 0) {
            return;
        }

        if (index == 0) {
            if (isFirstSegmentOrthogonal) {
                propagateUp(index, dx, dy, getDefaultHeadOffset());
            } else {
                point.setX(point.getX() + dx);
                point.setY(point.getY() + dy);
            }
        } else {
            if (isLastSegmentOrthogonal) {
                propagateDown(index, dx, dy, getDefaultTailOffset());
            } else {
                point.setX(point.getX() + dx);
                point.setY(point.getY() + dy);
            }
        }

        // infer();
        Point2DArray corrected = correctComputedPoints(points, nonOrthogonalPoints);
        setPoints(corrected);

        // refresh();
        // batch();
    }

    public void propagateUp(int index, double dx, double dy, double min) {
        if (dx == 0 && dy == 0) {
            return;
        }
        if (index >= (points.size() -1)) {
            return;
        }
        int nextIndex = index + 1;
        Point2D candidate = points.get(index);
        Point2D next = points.get(nextIndex);

        // DomGlobal.console.log("NON-ORTHO-POINTS = " + nonOrthogonalPoints);
        // DomGlobal.console.log("upIndexesToRecalculate = " + upIndexesToRecalculate);
        boolean isHorizontal = false;
        boolean isVertical = false;
        if (!nonOrthogonalPoints.contains(next)) {
            // DomGlobal.console.log("PROPAGATING");
            isHorizontal = isHorizontal(candidate, next);
            isVertical = isVertical(candidate, next);
            double px = 0;
            double py = 0;

            boolean isNextLast = points.size() > 2 && nextIndex >= (points.size() - 1);
            boolean isFirstOrLast = index < 1 || isNextLast;
            double segmentMin = isFirstOrLast ? min : 0;

            Point2D last = null;
            if (index == 0 || isNextLast) {
                last = points.get(points.size() - 1);
            }

            // TODO: This offset only applies when both head/tail directions are in opposite direction...
            final double offset = getDefaultHeadOffset() + getDefaultTailOffset();
            if (isHorizontal) {
                px = propagateOrthogonalSegmentUp(candidate.getX(), next.getX(), dx, segmentMin, null != last ? last.getX() - offset : null);
                py = dy;
                dx = isNextLast && px != 0 ? 0 : dx;
                dy = !isNextLast ? dy : 0;
            } else if (isVertical) {
                dx = !isNextLast ? dx : 0;
                px = dx;
                py = propagateOrthogonalSegmentUp(candidate.getY(), next.getY(), dy, segmentMin, null != last ? last.getY() - offset : null);
                dy = isNextLast && py != 0 ? 0 : dy;
            } else {
                // No need to propagate on no orthogonal segments
                px = 0;
                py = 0;
            }

            boolean propagate = px != 0 || py != 0;
            if (propagate) {
                // DomGlobal.console.log("PROPAGATING UP TO [" + nextIndex + "]");
                propagateUp(nextIndex, px, py, min);
            }
        } else {
            // DomGlobal.console.log("NON PROPAGATING [" + (index + 1) + "]");
        }


        if (dx != 0 || dy != 0) {
            // DomGlobal.console.log("SETTING POINT [" + index + "] to [" + (candidate.getX() + dx) + ", " + (candidate.getY() + dy) + "]");
            candidate.setX(candidate.getX() + dx);
            candidate.setY(candidate.getY() + dy);
        }

        if (isHorizontal) {
            if ((dy != 0) && (candidate.getY() != next.getY())) {
                upIndexesToRecalculate.add(index);
            }
        }
        if (isVertical) {
            if (dx != 0 && (candidate.getX() != next.getX())) {
                upIndexesToRecalculate.add(index);
            }
        }

    }

    private double propagateOrthogonalSegmentUp(double candidate, double next, double dist, double min, Double lastValue) {
        double p = 0;
        double ad = Math.abs(next - candidate);
        double cx = candidate + dist;
        double d = next > candidate ? next - cx : cx - next;
        boolean grows = d > Math.abs(ad);

        if (d >= min && !grows) {
            // do not propagate
            p = 0;
        }
        if (d < min && !grows) {
            // propagate?
            p = (cx + (min * (next > candidate ? 1 : -1))) - next;
        }
        if (d < min && grows) {
            // don't propagate, will propagate once d = 0 & grows, if necessary
            p = 0;
        }
        if (d >= min && grows) {
            p = 0;
            // Propagate back
            if (null != lastValue) {
                if (cx < lastValue) {
                    // If last point is: after -> do not propagate
                    p = candidate < lastValue ? 0 : lastValue - candidate;
                } else {
                    // If last point is: before -> propagate?
                    p = dist;
                }
            }
        }
        return p;
    }

    public void propagateDown(int index, double dx, double dy, double min) {
        if (dx == 0 && dy == 0) {
            return;
        }
        if (index < 1) {
            return;
        }

        int nextIndex = index - 1;
        Point2D candidate = points.get(index);
        Point2D next = points.get(nextIndex);

        boolean isHorizontal = false;
        boolean isVertical = false;
        if (!nonOrthogonalPoints.contains(next)) {
            isHorizontal = isHorizontal(candidate, next);
            isVertical = isVertical(candidate, next);
            double px = 0;
            double py = 0;

            boolean isNextFirst = points.size() > 2 && nextIndex < 1;
            double segmentMin = index >= (points.size() - 1) || isNextFirst ? min : 0;

            Point2D first = null;
            if (points.size() > 2 && (index == (points.size() - 1) || isNextFirst)) {
                first = points.get(0);
            }

            // TODO: This offset only applies when both head/tial directions are in opposite direction...
            final double offset = getDefaultHeadOffset() + getDefaultTailOffset();
            if (isHorizontal) {
                px = propagateOrthogonalSegmentDown(candidate.getX(), next.getX(), dx, segmentMin, null != first ? first.getX() + offset : null);
                py = dy;
                dx = isNextFirst && px != 0 ? 0 : dx;
                dy = !isNextFirst ? dy : 0;
            } else if (isVertical) {
                dx = !isNextFirst ? dx : 0;
                px = dx;
                py = propagateOrthogonalSegmentDown(candidate.getY(), next.getY(), dy, segmentMin, null != first ? first.getY() + offset : null);
                dy = isNextFirst && py != 0 ? 0 : dy;
            } else {
                // No need to propagate on no orthogonal segments
                px = 0;
                py = 0;
            }

            boolean propagate = px != 0 || py != 0;
            if (propagate) {
                // DomGlobal.console.log("PROPAGATING DOWN TO [" + nextIndex + "]");
                propagateDown(nextIndex, px, py, min);
            }
        }

        if (dx != 0 || dy != 0) {
            // DomGlobal.console.log("SETTING POINT [" + index + "] to [" +  (candidate.getX() + dx) + ", " + (candidate.getY() + dy) + "]");
            candidate.setX(candidate.getX() + dx);
            candidate.setY(candidate.getY() + dy);
        }

        if (isHorizontal) {
            if ((dy != 0) && (candidate.getY() != next.getY())) {
                upIndexesToRecalculate.add(index - 1);
            }
        }
        if (isVertical) {
            if (dx != 0 && (candidate.getX() != next.getX())) {
                upIndexesToRecalculate.add(index - 1);
            }
        }

    }

    private double propagateOrthogonalSegmentDown(double candidate, double next, double dist, double min, Double lastValue) {
        double p = 0;
        double ad = Math.abs(next - candidate);
        double cx = candidate + dist;
        double d = next > candidate ? next - cx : cx - next;
        boolean grows = d > Math.abs(ad);

        if (d >= min && !grows) {
            // do not propagate
            p = 0;
        }
        if (d < min && !grows) {
            // propagate?
            p = (cx + (min * (next > candidate ? 1 : -1))) - next;
        }
        if (d < min && grows) {
            // don't propagate, will propagate once d = 0 & grows, if necessary
            p = 0;
        }
        if (d >= min && grows) {
            p = 0;
            // Propagate back
            if (null != lastValue) {
                // TODO: Here comparison differs from propagateOrthogonalSegmentUp
                if (cx > lastValue) {
                    // If last point is: after -> do not propagate
                    p = candidate > lastValue ? 0 : lastValue - candidate;
                } else {
                    // If last point is: before -> propagate?
                    p = dist;
                }
            }
        }
        return p;
    }

    public Point2DArray inferOrthogonalSegments(Direction headDirection, Direction tailDirection, double headOffset, double tailOffset) {
        Point2DArray result = new Point2DArray();

        // TODO: Use a copy or not?
        Point2DArray copy = points;
        int size = copy.size();
        for (int i = 0; i < size; i++) {
            if (upIndexesToRecalculate.contains(i) && (i < size - 1)) {
                boolean isFirstOrLastPoint = (i == 0 || i == (size - 1));
                headOffset = isFirstOrLastPoint ? headOffset : 0;
                tailOffset = isFirstOrLastPoint ? tailOffset : 0;
                Point2DArray inferred = inferOrthogonalSegments(copy, i, headDirection, tailDirection, headOffset, tailOffset);
                DomGlobal.console.log("REBUILDING ORTHOGONAL POINTS FOR INDEX [" + i + "] = [" + inferred + "]");
                for (int j = 0; j < inferred.size(); j++) {
                    Point2D o = inferred.get(j);
                    result.push(o);
                }
                i++;
            } else {
                Point2D point = copy.get(i);
                result.push(point);
            }
        }

        return result;
    }

    public static Point2DArray inferOrthogonalSegments(Point2DArray copy, int index, Direction headDirection, Direction tailDirection, double headOffset, double tailOffset) {
        Point2D p0 = copy.get(index);
        Point2D p1 = copy.get(index + 1);
        return inferOrthogonalSegments(p0, p1, headDirection, tailDirection, headOffset, tailOffset);
    }

    public static Point2DArray inferOrthogonalSegments(Point2D p0, Point2D p1, Direction headDirection, Direction tailDirection, double headOffset, double tailOffset) {
        Point2DArray result = new Point2DArray();
        result.push(p0);
        if (isOrthogonal(p0, p1)) {
            result.push(p1);
            return result;
        }
        Point2DArray ps = new Point2DArray();
        ps.push(p0.copy());
        ps.push(p1.copy());
        NFastDoubleArray p = drawOrthogonalLinePoints(ps, headDirection, tailDirection, 0, headOffset, tailOffset, true);
        Point2DArray array = Point2DArray.fromNFastDoubleArray(p);
        array = correctComputedPoints(array, Collections.<Point2D>emptyList());
        if (array.size() > 2) {
            for (int j = (headOffset != 0 ? 0 : 1); j < array.size(); j++) {
                Point2D op = array.get(j);
                result.push(op);
            }
        }
        if (!p1.equals(result.get(result.size() - 1))) {
            result.push(p1);
        }
        return result;
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

    public static Point2DArray correctComputedPoints(Point2DArray points, List<Point2D> nonOrthogonalPoints) {
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
                if (!nonOrthogonalPoints.contains(p0)) {
                    if (ref.getX() == p0.getX() && p0.getX() == p1.getX()) {
                        write = false;
                    }
                    if (ref.getY() == p0.getY() && p0.getY() == p1.getY()) {
                        write = false;
                    }
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
                DomGlobal.console.error("Invalid Direction " + direction);
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

    private static final NFastDoubleArray drawOrthogonalLinePoints(final Point2DArray points,  Direction headDirection, Direction tailDirection,
                                                                   final double correction, double headOffset, double tailOffset, boolean write) {
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

        if (points.size() == 2 || (points.size() > 2 && isOrthogonal(p0, p1))) {
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
                DomGlobal.console.error("This should not be reached (Defensive Code)");
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
                DomGlobal.console.error("Invalid Direction " + tailDirection);
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
                    DomGlobal.console.error("Invalid Direction " + tailDirection);
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
                    DomGlobal.console.error("Invalid Direction " + tailDirection);
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

    public OrthogonalNewPolyLine setCornerRadius(final double radius) {
        this.cornerRadius = cornerRadius;

        return refresh();
    }

    public double getBreakDistance() {
        return m_breakDistance;
    }

    public OrthogonalNewPolyLine setBreakDistance(double distance) {
        m_breakDistance = distance;

        return refresh();
    }

    @Override
    public OrthogonalNewPolyLine setPoint2DArray(final Point2DArray points) {
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
    public Shape<OrthogonalNewPolyLine> copyTo(Shape<OrthogonalNewPolyLine> other) {
        super.copyTo(other);
        ((OrthogonalNewPolyLine) other).m_headOffsetPoint = m_headOffsetPoint.copy();
        ((OrthogonalNewPolyLine) other).m_tailOffsetPoint = m_tailOffsetPoint.copy();
        ((OrthogonalNewPolyLine) other).m_computedPoint2DArray = m_computedPoint2DArray.copy();
        ((OrthogonalNewPolyLine) other).m_breakDistance = m_breakDistance;
        ((OrthogonalNewPolyLine) other).cornerRadius = cornerRadius;

        return other;
    }

    @Override
    public OrthogonalNewPolyLine cloneLine() {
        OrthogonalNewPolyLine orthogonalPolyLine = new OrthogonalNewPolyLine(this.getControlPoints().copy(), cornerRadius);
        return (OrthogonalNewPolyLine) copyTo(orthogonalPolyLine);
    }
}