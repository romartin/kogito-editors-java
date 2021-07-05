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
import com.ait.lienzo.client.core.shape.OrthogonalGroupPolyLine;
import com.ait.lienzo.client.core.shape.OrthogonalMultipointPolyLine;
import com.ait.lienzo.client.core.shape.OrthogonalPolyLine;
import com.ait.lienzo.client.core.shape.PolyLine;
import com.ait.lienzo.client.core.shape.VHPolyLine;
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
import org.kie.lienzo.client.util.WiresUtils;

import static com.ait.lienzo.client.core.shape.wires.IControlHandle.ControlHandleStandardType.CONNECTOR;
import static com.ait.lienzo.client.core.shape.wires.IControlHandle.ControlHandleStandardType.POINT;

/*
50 50
100 100
150 50
200 50
200 100
 */

public class BasicLinesExample extends BaseExample implements Example {

    private static final String LINE_ORTHOGONAL = "orthogonal";
    private static final String LINE_ORTHOGONAL_MULTIPOINT = "orthogonalMP";
    private static final String LINE_POLYLINE = "polyline";
    private static final String LINE_GROUP = "group";

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
    private HTMLButtonElement testButton;

    public BasicLinesExample(final String title) {
        super(title);
    }

    private static final int TYPE_POLYLINE = 0;
    private static final int TYPE_ORTHOGONAL = 1;
    private static final int TYPE_ORTHOGONAL_MULTIPOINT = 2;
    private static final int TYPE_ORTHOGONAL_GROUP = 3;

    @Override
    public void init(LienzoPanel panel,
                     HTMLDivElement topDiv) {
        super.init(panel, topDiv);
        topDiv.style.display = Style.Display.INLINE.getCssName();

        // pointsElement = createText("50 50\n100 50", this::onPointsTextChanged);
        pointsElement = createText("50 50\n100 50\n100 100\n150 100", this::onPointsTextChanged);
        // pointsElement = createText("50 50\n100 50\n100 100\n150 100\n150 150\n200 150", this::onPointsTextChanged);
        // pointsElement = createText("50 50\n100 100\n150 50\n200 50\n200 100", this::onPointsTextChanged);
        pointsElement.rows = 6;
        topDiv.appendChild(pointsElement);

        lineTypeElement = createSelect(lineTypesMap(), this::onLineTypeChanged);
        lineTypeElement.selectedIndex = TYPE_ORTHOGONAL_MULTIPOINT;
        topDiv.appendChild(lineTypeElement);

        headDirectionElement = createSelect(directionsMap(), this::onHeadDirectionChanged);
        headDirectionElement.selectedIndex = 3;
        topDiv.appendChild(headDirectionElement);

        tailDirectionElement = createSelect(directionsMap(), this::onTailDirectionChanged);
        tailDirectionElement.selectedIndex = 4;
        topDiv.appendChild(tailDirectionElement);

        refreshButton = createButton("Refresh", this::onRefreshButtonClick);
        topDiv.appendChild(refreshButton);

        logPointsButton = createButton("Log", this::onLogPointsButtonClick);
        topDiv.appendChild(logPointsButton);

        showPointsButton = createButton("Show POINTS", this::onShowPointsButton);
        topDiv.appendChild(showPointsButton);

        showHandlesButton = createButton("Show HANDLES", this::onShowHandlesButton);
        topDiv.appendChild(showHandlesButton);

        hideCPButton = createButton("Hide CP", this::onHidePointHandlesButton);
        topDiv.appendChild(hideCPButton);

        testButton = createButton("Test", this::onTestButtonClick);
        topDiv.appendChild(testButton);

        pointsContainer = new Group();
        layer.add(pointsContainer);
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
        wiresManager.setDockingAcceptor(IDockingAcceptor.NONE);
        wiresManager.setConnectionAcceptor(IConnectionAcceptor.ALL);
        wiresManager.setControlPointsAcceptor(IControlPointsAcceptor.ALL);
    }

    private void onTestButtonClick() {
        // onTestPoints();
        //testLogPoints();
        testUpdatePx();
    }

    private void testUpdatePx() {
        if (line instanceof OrthogonalMultipointPolyLine) {
            Point2D p1 = ((OrthogonalMultipointPolyLine) line).getPoints().get(((OrthogonalMultipointPolyLine) line).getPoints().size() - 1);
            ((OrthogonalMultipointPolyLine) line).update(p1, p1.getX(), p1.getY() + 5);
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
        onShowPointHandlesButton(CONNECTOR);
    }

    private void onShowPointsButton() {
        onShowPointHandlesButton(POINT);
    }

    private void onShowPointHandlesButton(IControlHandle.ControlHandleType type) {
        clearPointsHandles();
        pointHandles = line.getControlHandles(type).get(type);
        pointHandles.show();
        registerHandles();
    }

    private void registerHandles() {
        for (int i = 0; i < pointHandles.size(); i++) {
            IControlHandle ch = pointHandles.getHandle(i);
            IPrimitive<?> prim = ch.getControl();
            prim.addNodeDragEndHandler(new NodeDragEndHandler() {
                @Override
                public void onNodeDragEnd(NodeDragEndEvent event) {
                    DomGlobal.console.log("Points changed!");
                    fillPoints();
                }
            });
        }
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
        refreshComputedPoints();
        refreshPoints();
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
        }
        if (LINE_ORTHOGONAL_MULTIPOINT.equals(lineTypeElement.value)) {
            refreshOrthogonalMultipointLine();
        } else if (LINE_POLYLINE.equals(lineTypeElement.value)) {
            refreshPolyLine();
        } else if (LINE_GROUP.equals(lineTypeElement.value)) {
            refreshGroupLine();
        }
        refreshWiresStuff();
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
        } else if (line instanceof OrthogonalGroupPolyLine) {
            points = ((OrthogonalGroupPolyLine) line).getComputedPoint2DArray();
        } else if (line instanceof OrthogonalMultipointPolyLine) {
            points = ((OrthogonalMultipointPolyLine) line).getComputedPoint2DArray();
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

    private void refreshGroupLine() {
        Point2DArray points = parsePoints();
        // Direction headDirection = parseHeadDirection();
        // Direction tailDirection = parseTailDirection();
        OrthogonalGroupPolyLine line = new OrthogonalGroupPolyLine().setPoint2DArray(points);
        this.line = line;
        layer.add(line);
    }

    private void refreshPolyLine() {
        Point2DArray points = parsePoints();
        Direction headDirection = parseHeadDirection();
        Direction tailDirection = parseTailDirection();
        VHPolyLine line = WiresUtils.createPolyline(points);
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

    private void refreshOrthogonalMultipointLine() {
        Point2DArray points = parsePoints();
        Direction headDirection = parseHeadDirection();
        Direction tailDirection = parseTailDirection();
        OrthogonalMultipointPolyLine line = WiresUtils.createOrthogonalMultipointPolyline(points);
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
        options.put(TYPE_ORTHOGONAL_MULTIPOINT + "_" + LINE_ORTHOGONAL_MULTIPOINT, LINE_ORTHOGONAL_MULTIPOINT);
        options.put(TYPE_ORTHOGONAL_GROUP + "_" + LINE_GROUP, LINE_GROUP);
        return options;
    }
}
