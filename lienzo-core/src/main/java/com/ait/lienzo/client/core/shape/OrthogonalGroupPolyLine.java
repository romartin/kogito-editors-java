/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ait.lienzo.client.core.shape;

import java.util.HashMap;
import java.util.Map;

import com.ait.lienzo.client.core.config.LienzoCore;
import com.ait.lienzo.client.core.event.NodeDragEndEvent;
import com.ait.lienzo.client.core.event.NodeDragEndHandler;
import com.ait.lienzo.client.core.shape.storage.IStorageEngine;
import com.ait.lienzo.client.core.shape.storage.PrimitiveFastArrayStorageEngine;
import com.ait.lienzo.client.core.shape.wires.IControlHandle;
import com.ait.lienzo.client.core.shape.wires.IControlHandleList;
import com.ait.lienzo.client.core.types.PathPartEntryJSO;
import com.ait.lienzo.client.core.types.PathPartList;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.lienzo.client.core.types.Point2DArray;
import com.ait.lienzo.shared.core.types.Color;
import com.ait.lienzo.shared.core.types.Direction;
import com.ait.lienzo.shared.core.types.GroupType;
import elemental2.core.JsArray;
import elemental2.dom.DomGlobal;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsProperty;

// TODO: Implement / override those:
// refresh()
// getComputedBoundingPoints()
// getBoundingBox
// setEventPropagationMode(EventPropagationMode.FIRST_ANCESTOR)
// setDraggable(true)
// getControlHandles
// getComputedLocation
// addNodeMouseClickHandler...

/*
   Composition Strategy:
   =====================
       - if only 2 points in line, or in fist / last segments, if are V/H -> Orthogonal
       - if at least 3 points forming V/H segments -> OrthogonalMultipoint
       - otherwise -> Poly

   Interactions:
   =============
       - When moving source / target nodes -> setting new bendpoints location for same line type
       - When asking for HANDLE -> delegate to each line type
       - When asking for POINT -> convert into polyine
*/

public class OrthogonalGroupPolyLine
        extends GroupOf<IPrimitive<?>, OrthogonalGroupPolyLine>
        implements IDirectionalMultiPointShape<OrthogonalGroupPolyLine> {

    @JsProperty
    private Point2DArray points;

    @JsProperty
    private JsArray<AbstractDirectionalMultiPointShape> lines = new JsArray<>();

    @JsProperty
    private Direction headDirection;

    @JsProperty
    private Direction tailDirection;

    @JsProperty
    private double correctionOffset = LienzoCore.get().getDefaultConnectorOffset();

    @JsProperty
    private double headOffset;

    @JsProperty
    private double tailOffset;

    public static void provaaa() {
        Group group = null;
        PolyLine polyLine = null;
        OrthogonalPolyLine orthogonalPolyLine = null;
    }

    private final PathPartList pathPartList = new PathPartList();

    public static JsArray<AbstractDirectionalMultiPointShape> build(Point2DArray points) {
        JsArray<AbstractDirectionalMultiPointShape> lines = new JsArray<>();
        int size = points.size();
        if (2 <= points.size()) {
            Point2DArray actualPoints = new Point2DArray();
            boolean isActualOrtho = false;
            actualPoints.push(points.get(0));
            for (int i = 1; i < size; i++) {
                Point2D p0 = points.get(i - 1);
                Point2D p1 = points.get(i);
                boolean isHorizontal = p0.getY() == p1.getY();
                boolean isVertical = p0.getX() == p1.getX();
                boolean isHV = isHorizontal || isVertical;
                boolean segmentTypeChanged = isActualOrtho != isHV;
                if (i > 1 && segmentTypeChanged) {
                    pushLine(lines, actualPoints, isActualOrtho);
                    actualPoints = new Point2DArray();
                    actualPoints.push(p0);
                }
                isActualOrtho = isHV;
                actualPoints.push(p1);
            }
            if (actualPoints.size() > 1) {
                pushLine(lines, actualPoints, isActualOrtho);
            }
        }
        return lines;
    }

    private static void pushLine(JsArray<AbstractDirectionalMultiPointShape> lines, Point2DArray points, boolean isOrthogonal) {
        DomGlobal.console.log("Creating " + (isOrthogonal ? "ortho" : "poly") + " line with points " + points);
        AbstractDirectionalMultiPointShape l = isOrthogonal ? createOrthogonalPolyline(points) : createPolyline(points);
        lines.push(l);
    }

    public OrthogonalGroupPolyLine() {
        super(GroupType.GROUP, new PrimitiveFastArrayStorageEngine());
    }

    private void build() {
        clear();
        lines = build(points);
        lines.forEach(new JsArray.ForEachCallbackFn<AbstractDirectionalMultiPointShape>() {
            @Override
            public Object onInvoke(AbstractDirectionalMultiPointShape line, int p1, JsArray<AbstractDirectionalMultiPointShape> p2) {
                add(line);
                PathPartList lineParts = line.getPathPartList();
                for (int i = 0; i < lineParts.size(); i++) {
                    PathPartEntryJSO linePart = lineParts.get(i);
                    pathPartList.push(linePart);
                }
                return null;
            }
        });
    }

    private void clear() {
        lines.forEach(new JsArray.ForEachCallbackFn<AbstractDirectionalMultiPointShape>() {
            @Override
            public Object onInvoke(AbstractDirectionalMultiPointShape line, int p1, JsArray<AbstractDirectionalMultiPointShape> p2) {
                line.removeFromParent();
                return null;
            }
        });
        lines = new JsArray<AbstractDirectionalMultiPointShape>();
        pathPartList.clear();
        removeAll();
    }

    @Override
    public OrthogonalGroupPolyLine refresh() {
        lines.forEach(new JsArray.ForEachCallbackFn<AbstractDirectionalMultiPointShape>() {
            @Override
            public Object onInvoke(AbstractDirectionalMultiPointShape line, int p1, JsArray<AbstractDirectionalMultiPointShape> p2) {
                line.refresh();
                return null;
            }
        });
        return this;
    }

    @Override
    public Map<IControlHandle.ControlHandleType, IControlHandleList> getControlHandles(IControlHandle.ControlHandleType... types) {
        if (types.length != 1) {
            // TODO
            throw new UnsupportedOperationException();
        }
        IControlHandle.ControlHandleType controlHandleType = types[0];
        if (IControlHandle.ControlHandleStandardType.HANDLE.equals(controlHandleType)) {

            // TODO Return handles for each line..
            return getControlHandlesForFirstLine(types);

        } else if (IControlHandle.ControlHandleStandardType.POINT.equals(controlHandleType)) {

            // Convert into a polyline and use its handles
            // TODO clear();
            final PolyLine pointsLine = createPolyline(points);
            add(pointsLine);
            Map<IControlHandle.ControlHandleType, IControlHandleList> handles = pointsLine.getControlHandles(controlHandleType);
            IControlHandleList handle = handles.get(controlHandleType);
            registerHandles(handle, pointsLine);
            return handles;

        } else {
            // TODO
            throw new UnsupportedOperationException();
        }

    }

    private Map<IControlHandle.ControlHandleType, IControlHandleList> getControlHandlesForFirstLine(IControlHandle.ControlHandleType... types) {
        final HashMap<IControlHandle.ControlHandleType, IControlHandleList> ret = new HashMap<>();
        final AbstractDirectionalMultiPointShape l = lines.getAt(0);
        IControlHandleList handle = (IControlHandleList) l.getControlHandles(types[0]).get(types[0]);
        ret.put(types[0], handle);
        registerHandles(handle, l);
        return ret;
    }

    private void registerHandles(final IControlHandleList handle, final AbstractDirectionalMultiPointShape l) {
        for (int i = 0; i < handle.size(); i++) {
            IControlHandle ch = handle.getHandle(i);
            IPrimitive<?> prim = ch.getControl();
            prim.addNodeDragEndHandler(new NodeDragEndHandler() {
                @Override
                public void onNodeDragEnd(NodeDragEndEvent event) {
                    DomGlobal.console.log("OrthoGroup - Points changed!");
                    Point2DArray points = l.getPoints();
                    DomGlobal.console.log("Points delegate [" + points + "]");
                    DomGlobal.console.log("Points composed [" + getPoint2DArray() + "]");

                    Point2DArray newPoints = getPoint2DArray().copy();
                    if (l instanceof OrthogonalPolyLine) {
                        Point2DArray computedPoint2DArray = ((OrthogonalPolyLine) l).getComputedPoint2DArray();
                        DomGlobal.console.log("Points composed - computed [" + computedPoint2DArray + "]");
                        newPoints = computedPoint2DArray.copy();
                    }

                    // TODO: Rebuild line
                    setPoint2DArray(newPoints);
                }
            });
        }
    }

    @JsFunction
    public interface PointsChangedCallbackFn {
        void onInvoke();
    }

    @Override
    public boolean isPathPartListPrepared() {
        final boolean[] prepared = {true};
        lines.forEach(new JsArray.ForEachCallbackFn<AbstractDirectionalMultiPointShape>() {
            @Override
            public Object onInvoke(AbstractDirectionalMultiPointShape line, int p1, JsArray<AbstractDirectionalMultiPointShape> p2) {
                if (!line.isPathPartListPrepared()) {
                    prepared[0] = false;
                }
                return null;
            }
        });
        return prepared[0];
    }

    @Override
    public PathPartList getPathPartList() {
        return pathPartList;
    }

    @Override
    public OrthogonalGroupPolyLine setPoint2DArray(Point2DArray points) {
        this.points = points;
        build();
        return this;
    }

    @Override
    public Point2DArray getPoint2DArray() {
        return points;
    }

    public Point2DArray getComputedPoint2DArray() {
        // TODO
        AbstractDirectionalMultiPointShape l = lines.getAt(0);
        if (l instanceof  OrthogonalPolyLine) {
            return ((OrthogonalPolyLine) l).getComputedPoint2DArray();
        }
        return new Point2DArray();
    }

    @Override
    public Point2D adjustPoint(double x, double y, double deltaX, double deltaY) {
        // TODO
        return new Point2D(x + deltaX, y + deltaX);
    }

    @Override
    public double getStrokeWidth() {
        // TODO
        return 0;
    }

    @Override
    public OrthogonalGroupPolyLine setStrokeColor(String stroke) {
        // TODO
        return this;
    }

    @Override
    public OrthogonalGroupPolyLine cloneLine() {
        return new OrthogonalGroupPolyLine().setPoint2DArray(points.copy());
    }

    @Override
    public OrthogonalGroupPolyLine setSelectionStrokeOffset(double offset) {
        // TODO
        return this;
    }

    @Override
    public OrthogonalGroupPolyLine setStrokeWidth(double width) {
        // TODO
        return this;
    }

    @Override
    public boolean isControlPointShape() {
        return true;
    }

    @Override
    public IOffsetMultiPointShape<?> asOffsetMultiPointShape() {
        return this;
    }

    @Override
    public IDirectionalMultiPointShape<?> asDirectionalMultiPointShape() {
        return this;
    }

    @Override
    public OrthogonalGroupPolyLine setTailDirection(Direction direction) {
        this.tailDirection = direction;
        // TODO
        return this;
    }

    @Override
    public Direction getTailDirection() {
        return tailDirection;
    }

    @Override
    public OrthogonalGroupPolyLine setHeadDirection(Direction direction) {
        this.headDirection = direction;
        // TODO
        return this;
    }

    @Override
    public Direction getHeadDirection() {
        return headDirection;
    }

    @Override
    public double getCorrectionOffset() {
        return correctionOffset;
    }

    @Override
    public OrthogonalGroupPolyLine setCorrectionOffset(double offset) {
        this.correctionOffset = offset;
        // TODO
        return this;
    }

    @Override
    public double getTailOffset() {
        return tailOffset;
    }

    @Override
    public OrthogonalGroupPolyLine setTailOffset(double offset) {
        this.tailOffset = offset;
        //  TODO
        return this;
    }

    @Override
    public Point2D getTailOffsetPoint() {
        // TODO
        return new Point2D(0, 0);
    }

    @Override
    public double getHeadOffset() {
        // TODO
        return headOffset;
    }

    @Override
    public OrthogonalGroupPolyLine setHeadOffset(double offset) {
        this.headOffset = offset;
        //  TODO
        return this;
    }

    @Override
    public Point2D getHeadOffsetPoint() {
        // TODO
        return new Point2D(0, 0);
    }

    public static OrthogonalPolyLine createOrthogonalPolyline(final double... points) {
        return createOrthogonalPolyline(Point2DArray.fromArrayOfDouble(points));
    }

    public static OrthogonalPolyLine createOrthogonalPolyline(final Point2DArray points) {
        return new OrthogonalPolyLine(points).setCornerRadius(5).setDraggable(true)
                .setStrokeWidth(2)
                .setHeadDirection(Direction.NONE).setTailDirection(Direction.NONE)
                .setStrokeColor(Color.getRandomHexColor());
    }

    public static PolyLine createPolyline(final double... points) {
        return createPolyline(Point2DArray.fromArrayOfDouble(points));
    }

    public static PolyLine createPolyline(final Point2DArray points) {
        return new PolyLine(points).setCornerRadius(5).setDraggable(true)
                .setStrokeWidth(2)
                .setHeadDirection(Direction.NONE).setTailDirection(Direction.NONE)
                .setStrokeColor(Color.getRandomHexColor());
    }

    @Override
    public IStorageEngine<IPrimitive<?>> getDefaultStorageEngine() {
        return new PrimitiveFastArrayStorageEngine();
    }
}
