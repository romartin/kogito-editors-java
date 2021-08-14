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

package org.kie.lienzo.client;

import java.util.HashMap;
import java.util.Map;

import com.ait.lienzo.client.core.event.NodeDragEndEvent;
import com.ait.lienzo.client.core.event.NodeDragEndHandler;
import com.ait.lienzo.client.core.shape.Circle;
import com.ait.lienzo.client.core.shape.Group;
import com.ait.lienzo.client.core.shape.IDirectionalMultiPointShape;
import com.ait.lienzo.client.core.shape.IPrimitive;
import com.ait.lienzo.client.core.shape.MultiPath;
import com.ait.lienzo.client.core.shape.OrthogonalNewPolyLine;
import com.ait.lienzo.client.core.shape.OrthogonalPolyLine;
import com.ait.lienzo.client.core.shape.PolyLine;
import com.ait.lienzo.client.core.shape.wires.IConnectionAcceptor;
import com.ait.lienzo.client.core.shape.wires.IContainmentAcceptor;
import com.ait.lienzo.client.core.shape.wires.IControlHandle;
import com.ait.lienzo.client.core.shape.wires.IControlHandleList;
import com.ait.lienzo.client.core.shape.wires.IControlPointsAcceptor;
import com.ait.lienzo.client.core.shape.wires.IDockingAcceptor;
import com.ait.lienzo.client.core.shape.wires.ILocationAcceptor;
import com.ait.lienzo.client.core.shape.wires.WiresConnector;
import com.ait.lienzo.client.core.shape.wires.WiresManager;
import com.ait.lienzo.client.core.shape.wires.WiresShape;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.lienzo.client.core.types.Point2DArray;
import com.ait.lienzo.client.widget.panel.LienzoPanel;
import com.ait.lienzo.shared.core.types.ColorName;
import com.ait.lienzo.shared.core.types.Direction;
import com.ait.lienzo.shared.core.types.IColor;
import com.google.gwt.dom.client.Style;
import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLButtonElement;
import elemental2.dom.HTMLDivElement;
import elemental2.dom.HTMLSelectElement;
import elemental2.dom.HTMLTextAreaElement;
import elemental2.dom.Node;
import org.kie.lienzo.client.util.WiresUtils;

import static com.ait.lienzo.client.core.shape.wires.IControlHandle.ControlHandleStandardType.OFFSET;
import static com.ait.lienzo.client.core.shape.wires.IControlHandle.ControlHandleStandardType.POINT;

public class BasicLinesExample extends BaseExample implements Example {

    private static final String LINE_ORTHOGONAL = "orthogonal";
    private static final String LINE_ORTHOGONAL_NEW = "orthogonalNew";
    private static final String LINE_POLYLINE = "polyline";

    private WiresManager wiresManager;
    private WiresShape sourceShape;
    private WiresShape targetShape;
    private WiresConnector connector;
    private IDirectionalMultiPointShape line;
    private Group pointsContainer;
    private HTMLTextAreaElement pointsElement;
    private HTMLSelectElement lineTypeElement;
    private HTMLSelectElement headDirectionElement;
    private HTMLSelectElement tailDirectionElement;
    private HTMLButtonElement refreshButton;
    private HTMLButtonElement logPointsButton;
    private HTMLButtonElement showHandlesButton;
    private HTMLButtonElement showPointsButton;
    private HTMLButtonElement hideCPButton;
    private HTMLButtonElement upButton;
    private HTMLButtonElement downButton;
    private HTMLButtonElement leftButton;
    private HTMLButtonElement rightButton;

    public BasicLinesExample(final String title) {
        super(title);
    }

    private static final int TYPE_POLYLINE = 0;
    private static final int TYPE_ORTHOGONAL = 1;
    private static final int TYPE_ORTHOGONAL_NEW = 2;

    @Override
    public void init(LienzoPanel panel,
                     HTMLDivElement topDiv) {
        super.init(panel, topDiv);
        topDiv.style.display = Style.Display.INLINE.getCssName();

        pointsElement = createText("50 50\n100 50", this::onPointsTextChanged);
        // pointsElement = createText("50 50\n100 50\n100 100\n150 100", this::onPointsTextChanged);
        // pointsElement = createText("90 88\n100 88\n100 100\n110 100", this::onPointsTextChanged);
        // pointsElement = createText("50 50\n100 50\n100 100\n150 100\n150 150\n200 150", this::onPointsTextChanged);
        // pointsElement = createText("50 50\n100 100\n150 50\n200 50\n200 100", this::onPointsTextChanged);
        pointsElement.rows = 6;
        addToHeader(pointsElement);

        lineTypeElement = createSelect(lineTypesMap(), this::onLineTypeChanged);
        lineTypeElement.selectedIndex = TYPE_ORTHOGONAL_NEW;
        addToHeader(lineTypeElement);

        headDirectionElement = createSelect(directionsMap(), this::onHeadDirectionChanged);
        headDirectionElement.selectedIndex = 3;
        addToHeader(headDirectionElement);

        tailDirectionElement = createSelect(directionsMap(), this::onTailDirectionChanged);
        tailDirectionElement.selectedIndex = 4;
        addToHeader(tailDirectionElement);

        refreshButton = createButton("Refresh", this::onRefreshButtonClick);
        addToHeader(refreshButton);

        logPointsButton = createButton("Log", this::onLogPointsButtonClick);
        addToHeader(logPointsButton);

        showPointsButton = createButton("Show POINTS", this::onShowPointsButton);
        addToHeader(showPointsButton);

        showHandlesButton = createButton("Show HANDLES", this::onShowHandlesButton);
        addToHeader(showHandlesButton);

        hideCPButton = createButton("Hide CP", this::onHidePointHandlesButton);
        addToHeader(hideCPButton);

        upButton = createButton("Up", this::onUpButtonClick);
        addToHeader(upButton);

        downButton = createButton("Down", this::onDownButtonClick);
        addToHeader(downButton);

        leftButton = createButton("Left", this::onLeftButtonClick);
        addToHeader(leftButton);

        rightButton = createButton("Right", this::onRightButtonClick);
        addToHeader(rightButton);

        pointsContainer = new Group();
        layer.add(pointsContainer);
    }

    private static boolean SHOW_HEADER = true;

    private void addToHeader(Node newChild) {
        if (SHOW_HEADER) {
            topDiv.appendChild(newChild);
        }
    }

    @Override
    public void run() {
        initWires();
        refresh();
    }

    private void initWires() {
        wiresManager = WiresManager.get(layer);
        wiresManager.enableSelectionManager();
        wiresManager.setLocationAcceptor(ILocationAcceptor.ALL);
        wiresManager.setContainmentAcceptor(IContainmentAcceptor.ALL);
        wiresManager.setDockingAcceptor(IDockingAcceptor.ALL);
        wiresManager.setConnectionAcceptor(IConnectionAcceptor.ALL);
        wiresManager.setControlPointsAcceptor(IControlPointsAcceptor.ALL);
    }

    private void onUpButtonClick() {
        testUpdatePx(0, -1);
    }

    private void onDownButtonClick() {
        testUpdatePx(0, +1);
    }

    private void onLeftButtonClick() {
        testUpdatePx(-1, 0);
    }

    private void onRightButtonClick() {
        testUpdatePx(1, 0);
    }

    private void testUpdatePx(double dx, double dy) {
        if (line instanceof OrthogonalNewPolyLine) {
            int lastIndex = ((OrthogonalNewPolyLine) line).getPoints().size() - 1;
            Point2D p = ((OrthogonalNewPolyLine) line).getPoints().get(lastIndex);
            // Point2D p = ((OrthogonalNewPolyLine) line).getPoints().get(0);
            ((OrthogonalNewPolyLine) line).update(lastIndex, p.getX() + dx, p.getY() + dy);
            layer.draw();
            onLogPointsButtonClick();
        }
    }

    private void testLogPoints() {
        Point2DArray points = line.getPoint2DArray();
        DomGlobal.console.log("POINTS [" + points + "]");
    }

    private IControlHandleList pointHandles;

    private void onShowHandlesButton() {
        onShowPointHandlesButton(OFFSET);
    }

    private void onShowPointsButton() {
        onShowPointHandlesButton(POINT);
    }

    private void onShowPointHandlesButton(IControlHandle.ControlHandleType type) {
        clearPointsHandles();
        pointHandles = line.getControlHandles(type).get(type);
        pointHandles.show();
    }

    private void onHidePointHandlesButton() {
        clearPointsHandles();
        layer.batch();
    }

    private void clearPointsHandles() {
        pointsContainer.removeAll();
        if (null != pointHandles) {
            pointHandles.destroy();
            pointHandles = null;
        }
    }

    private void onLogPointsButtonClick() {
        pointsContainer.removeAll();
        // refreshComputedPoints();
        //refreshPoints();
        testLogPoints();
        layer.draw();
    }

    private void onRefreshButtonClick() {
        refresh();
    }

    private void onPointsTextChanged() {
        refresh();
    }

    private void onLineTypeChanged(String value) {
        refresh();
    }

    private void onHeadDirectionChanged(String value) {
        refresh();
    }

    private void onTailDirectionChanged(String value) {
        refresh();
    }

    private void refresh() {
        if (null != connector) {
            wiresManager.deregister(connector);
        }
        if (null != targetShape) {
            wiresManager.deregister(targetShape);
        }
        if (null != sourceShape) {
            wiresManager.deregister(sourceShape);
        }
        layer.removeAll();
        layer.clear();
        layer.add(pointsContainer);
        if (LINE_ORTHOGONAL.equals(lineTypeElement.value)) {
            refreshOrthogonalLine();
        } else if (LINE_ORTHOGONAL_NEW.equals(lineTypeElement.value)) {
            refreshOrthogonalNewPolyline();
        } else if (LINE_POLYLINE.equals(lineTypeElement.value)) {
            refreshPolyLine();
        }
        // refreshWiresStuff();
        layer.draw();
        onLogPointsButtonClick();
    }

    private void refreshWiresStuff() {
        sourceShape = createRect("r1");
        sourceShape.setLocation(new Point2D(100, 200));
        targetShape = createRect("r2");
        targetShape.setLocation(new Point2D(300, 200));
        connector = WiresUtils.connect(sourceShape.getMagnets(),
                                       3,
                                       targetShape.getMagnets(),
                                       7,
                                       wiresManager,
                                       true);
    }

    private WiresShape createRect(String id) {
        WiresShape shape = new WiresShape(new MultiPath().rect(0, 0, 100, 100)
                                                  .setStrokeColor("#FF0000")
                                                  .setFillColor("#FF0000"))
                .setDraggable(true);
        shape.getGroup().setID(id);
        shape.getGroup().setUserData(id);

        wiresManager.register(shape);
        wiresManager.getMagnetManager().createMagnets(shape);
        return shape;
    }

    private void refreshPoints() {
        fillPoints(parsePoints(), ColorName.BLACK, null, 2);
    }

    private void refreshComputedPoints() {
        Point2DArray points = null;
        if (line instanceof OrthogonalPolyLine) {
            points = ((OrthogonalPolyLine) line).getComputedPoint2DArray();
        } else if (line instanceof OrthogonalNewPolyLine) {
            points = ((OrthogonalNewPolyLine) line).getComputedPoint2DArray();
        } else if (line instanceof PolyLine) {
            points = ((PolyLine) line).getPoints();
        }
        if (null != points) {
            fillPoints(points, null, ColorName.RED, 5);
        }
    }

    private void fillPoints(Point2DArray points, IColor fillColor, IColor strokeColor, double radius) {
        points.getPoints().forEach(point -> {
            pointsContainer.add(new Circle(radius)
                                        .setStrokeColor(strokeColor)
                                        .setFillColor(fillColor)
                                        .setLocation(point));
        });
    }

    private void refreshPolyLine() {
        Point2DArray points = parsePoints();
        Direction headDirection = parseHeadDirection();
        Direction tailDirection = parseTailDirection();
        PolyLine line = WiresUtils.createPolyline(points);
        this.line = line;
        line.setHeadDirection(headDirection);
        line.setTailDirection(tailDirection);
        layer.add(line);
    }

    private void refreshOrthogonalLine() {
        Point2DArray points = parsePoints();
        Direction headDirection = parseHeadDirection();
        Direction tailDirection = parseTailDirection();
        OrthogonalPolyLine line = WiresUtils.createOrthogonalPolyline(points);
        this.line = line;
        line.setHeadDirection(headDirection);
        line.setTailDirection(tailDirection);
        layer.add(line);
    }

    private void refreshOrthogonalNewPolyline() {
        Point2DArray points = parsePoints();
        Direction headDirection = parseHeadDirection();
        Direction tailDirection = parseTailDirection();
        OrthogonalNewPolyLine line = WiresUtils.createOrthogonalNewPolyline(points);
        this.line = line;
        line.setHeadDirection(headDirection);
        line.setTailDirection(tailDirection);
        layer.add(line);
    }

    private Direction parseHeadDirection() {
        String value = headDirectionElement.value;
        return Direction.lookup(value);
    }

    private Direction parseTailDirection() {
        String value = tailDirectionElement.value;
        return Direction.lookup(value);
    }

    private Point2DArray parsePoints() {
        Point2DArray points = new Point2DArray();
        String[] lines = pointsElement.value.split("\n");
        for (String line : lines) {
            String[] point = line.split("\\s");
            if (point.length == 2) {
                double x = Double.parseDouble(point[0]);
                double y = Double.parseDouble(point[1]);
                points.push(new Point2D(x, y));
            }
        }
        return points;
    }

    private void fillPoints() {
        String raw = "";
        Point2DArray points = null;
        if (false && line instanceof OrthogonalPolyLine) {
            points = ((OrthogonalPolyLine) line).getComputedPoint2DArray();
        } else {
            points = line.getPoint2DArray();
        }
        for (int i = 0; i < points.size(); i++) {
            Point2D point = points.get(i);
            String x = Double.valueOf(point.getX()).toString();
            String y = Double.valueOf(point.getY()).toString();
            raw += x + " " + y + "\n";
        }
        pointsElement.textContent = raw;
        onPointsTextChanged();
    }

    private static Map<String, String> directionsMap() {
        Map<String, String> options = new HashMap<>();
        options.put(Direction.NONE.getValue(), Direction.NONE.getValue());
        options.put(Direction.NORTH.getValue(), Direction.NORTH.getValue());
        options.put(Direction.SOUTH.getValue(), Direction.SOUTH.getValue());
        options.put(Direction.EAST.getValue(), Direction.EAST.getValue());
        options.put(Direction.WEST.getValue(), Direction.WEST.getValue());
        return options;
    }

    private static Map<String, String> lineTypesMap() {
        Map<String, String> options = new HashMap<>();
        options.put(TYPE_POLYLINE + "_" + LINE_POLYLINE, LINE_POLYLINE);
        options.put(TYPE_ORTHOGONAL + "_" + LINE_ORTHOGONAL, LINE_ORTHOGONAL);
        options.put(TYPE_ORTHOGONAL_NEW + "_" + LINE_ORTHOGONAL_NEW, LINE_ORTHOGONAL_NEW);
        return options;
    }
}