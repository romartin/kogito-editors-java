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

package org.kie.workbench.common.stunner.client.lienzo.canvas.export;

import java.util.HashMap;
import java.util.Optional;

import com.ait.lienzo.client.core.types.DashArray;
import com.ait.lienzo.client.core.types.LinearGradient;
import com.ait.lienzo.client.core.types.PatternGradient;
import com.ait.lienzo.client.core.types.RadialGradient;
import com.ait.lienzo.client.core.types.Transform;
import com.ait.lienzo.shared.core.types.CompositeOperation;
import com.ait.lienzo.shared.core.types.LineCap;
import com.ait.lienzo.shared.core.types.LineJoin;
import com.ait.lienzo.shared.core.types.TextAlign;
import com.ait.lienzo.shared.core.types.TextBaseLine;
import com.ait.lienzo.test.LienzoMockitoTestRunner;
import com.google.gwt.core.client.GWT;
import elemental2.dom.Element;
import elemental2.dom.HTMLCanvasElement;
import elemental2.dom.Path2D;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.stunner.core.api.DefinitionManager;
import org.kie.workbench.common.stunner.core.client.canvas.AbstractCanvasHandler;
import org.kie.workbench.common.stunner.core.definition.adapter.AdapterManager;
import org.kie.workbench.common.stunner.core.definition.adapter.DefinitionSetAdapter;
import org.kie.workbench.common.stunner.core.diagram.Diagram;
import org.kie.workbench.common.stunner.core.diagram.Metadata;
import org.kie.workbench.common.stunner.core.graph.processing.index.Index;
import org.kie.workbench.common.stunner.core.registry.definition.TypeDefinitionSetRegistry;
import org.kie.workbench.common.stunner.core.util.UUID;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.uberfire.ext.editor.commons.client.file.exports.svg.IContext2D;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(LienzoMockitoTestRunner.class)
public class DelegateContext2DTest {

    public static final String NODE_UUID = UUID.uuid();

    @Mock
    private DelegateContext2D delegateContext2D;

    @Mock
    private IContext2D context;

    @Mock
    private DelegateContext2D.Converter nativeClassConverter;

    private LinearGradient.LinearGradientJSO linearGradientJSO;

    private PatternGradient.PatternGradientJSO patternGradientJSO;

    private RadialGradient.RadialGradientJSO radialGradientJSO;

    private HTMLCanvasElement htmlElement;

    private HTMLCanvasElement element;

    @Mock
    private AbstractCanvasHandler canvasHandler;

    @Mock
    private Diagram diagram;

    @Mock
    private Metadata metadata;

    private final String DEF_SET_ID = "DEF_SET_ID";

    @Mock
    private DefinitionManager definitionManager;

    @Mock
    private TypeDefinitionSetRegistry definitionSets;

    @Mock
    private Object defSet;

    @Mock
    private AdapterManager adapters;

    @Mock
    private DefinitionSetAdapter<Object> definitionSetAdapter;

    @Mock
    private Index graphIndex;

    @Mock
    private org.kie.workbench.common.stunner.core.graph.Element node;

    private final String SVG_NODE_ID = "node_id";

    @Before
    public void setUp() throws Exception {
        htmlElement = new HTMLCanvasElement();
        element = GWT.create(HTMLCanvasElement.class);
        when(nativeClassConverter.convert(any(Element.class), eq(HTMLCanvasElement.class)))
                .thenReturn(htmlElement);
        when(canvasHandler.getDiagram()).thenReturn(diagram);
        when(diagram.getMetadata()).thenReturn(metadata);
        when(metadata.getDefinitionSetId()).thenReturn(DEF_SET_ID);
        when(canvasHandler.getDefinitionManager()).thenReturn(definitionManager);
        when(definitionManager.definitionSets()).thenReturn(definitionSets);
        when(definitionSets.getDefinitionSetById(DEF_SET_ID)).thenReturn(defSet);
        when(definitionManager.adapters()).thenReturn(adapters);
        when(adapters.forDefinitionSet()).thenReturn(definitionSetAdapter);
        when(definitionSetAdapter.getSvgNodeId(defSet)).thenReturn(Optional.of(SVG_NODE_ID));
        when(canvasHandler.getGraphIndex()).thenReturn(graphIndex);
        when(graphIndex.get(NODE_UUID)).thenReturn(node);

        delegateContext2D.canvasHandler = canvasHandler;
        delegateContext2D.context = context;
        delegateContext2D.nativeClassConverter = nativeClassConverter;
    }

    @Test
    public void saveContainer() {
        doCallRealMethod().when(delegateContext2D).saveContainer(NODE_UUID);
        delegateContext2D.saveContainer(NODE_UUID);
        verify(context, times(1)).saveGroup(new HashMap<String, String>() {{
            put(null, NODE_UUID);
            put(delegateContext2D.DEFAULT_NODE_ID, NODE_UUID);
        }});
    }

    @Test
    public void restoreContainer() {
        doCallRealMethod().when(delegateContext2D).restoreContainer();
        delegateContext2D.restoreContainer();
        verify(context, times(1)).restoreGroup();
    }

    @Test
    public void save() {
        doCallRealMethod().when(delegateContext2D).save();
        delegateContext2D.save();
        verify(context, times(1)).saveStyle();
    }

    @Test
    public void restore() {
        doCallRealMethod().when(delegateContext2D).restore();
        delegateContext2D.restore();
        verify(context, times(1)).restoreStyle();
    }

    @Test
    public void beginPath() {
        doCallRealMethod().when(delegateContext2D).beginPath();
        delegateContext2D.beginPath();
        verify(context, times(1)).beginPath();
    }

    @Test
    public void closePath() {
        doCallRealMethod().when(delegateContext2D).closePath();
        delegateContext2D.closePath();
        verify(context, times(1)).closePath();
    }

    @Test
    public void moveTo() {
        doCallRealMethod().when(delegateContext2D).moveTo(anyInt(), anyInt());
        delegateContext2D.moveTo(1, 1);
        verify(context, times(1)).moveTo(1, 1);
    }

    @Test
    public void lineTo() {
        doCallRealMethod().when(delegateContext2D).lineTo(anyInt(), anyInt());
        delegateContext2D.lineTo(1, 1);
        verify(context, times(1)).lineTo(1, 1);
    }

    @Test
    public void setGlobalCompositeOperation() {
        doCallRealMethod().when(delegateContext2D).setGlobalCompositeOperation(any());
        delegateContext2D.setGlobalCompositeOperation(CompositeOperation.SOURCE_IN);
        verify(context, times(1))
                .setGlobalCompositeOperation(CompositeOperation.SOURCE_IN.getValue());
    }

    @Test
    public void setLineCap() {
        doCallRealMethod().when(delegateContext2D).setLineCap(any());
        delegateContext2D.setLineCap(LineCap.ROUND);
        verify(context, times(1)).setLineCap(LineCap.ROUND.getValue());
    }

    @Test
    public void setLineJoin() {
        doCallRealMethod().when(delegateContext2D).setLineJoin(any());
        delegateContext2D.setLineJoin(LineJoin.ROUND);
        verify(context, times(1)).setLineJoin(LineJoin.ROUND.getValue());
    }

    @Test
    public void quadraticCurveTo() {
        doCallRealMethod().when(delegateContext2D).quadraticCurveTo(anyInt(), anyInt(), anyInt(), anyInt());
        delegateContext2D.quadraticCurveTo(1, 1, 1, 1);
        verify(context, times(1)).quadraticCurveTo(1, 1, 1, 1);
    }

    @Test
    public void arc() {
        doCallRealMethod().when(delegateContext2D).arc(anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        delegateContext2D.arc(1, 1, 1, 1, 1);
        verify(context, times(1)).arc(1, 1, 1, 1, 1);
    }

    @Test
    public void arc1() {
        doCallRealMethod().when(delegateContext2D).arc(anyInt(),
                                                       anyInt(),
                                                       anyInt(),
                                                       anyInt(),
                                                       anyInt(),
                                                       anyBoolean());
        delegateContext2D.arc(1, 1, 1, 1, 1, true);
        verify(context, times(1))
                .arc(1, 1, 1, 1, 1, true);
    }

    @Test
    public void ellipse() {
        doCallRealMethod().when(delegateContext2D).ellipse(anyInt(),
                                                           anyInt(),
                                                           anyInt(),
                                                           anyInt(),
                                                           anyInt(),
                                                           anyInt(),
                                                           anyInt());
        delegateContext2D.ellipse(1, 1, 1, 1, 1, 1, 1);
        verify(context, times(1)).ellipse(1, 1, 1, 1, 1, 1, 1);
    }

    @Test
    public void ellipse1() {
        doCallRealMethod().when(delegateContext2D).ellipse(anyInt(),
                                                           anyInt(),
                                                           anyInt(),
                                                           anyInt(),
                                                           anyInt(),
                                                           anyInt(),
                                                           anyInt(),
                                                           anyBoolean());
        delegateContext2D.ellipse(1, 1, 1, 1, 1, 1, 1, true);
        verify(context, times(1)).ellipse(1, 1, 1, 1, 1, 1, 1, true);
    }

    @Test
    public void arcTo() {
        doCallRealMethod().when(delegateContext2D).arcTo(anyInt(),
                                                         anyInt(),
                                                         anyInt(),
                                                         anyInt(),
                                                         anyInt());
        delegateContext2D.arcTo(1, 1, 1, 1, 1);
        verify(context, times(1)).arcTo(1, 1, 1, 1, 1);
    }

    @Test
    public void bezierCurveTo() {
        doCallRealMethod().when(delegateContext2D).bezierCurveTo(anyInt(),
                                                                 anyInt(),
                                                                 anyInt(),
                                                                 anyInt(),
                                                                 anyInt(),
                                                                 anyInt());
        delegateContext2D.bezierCurveTo(1, 1, 1, 1, 1, 1);
        verify(context, times(1)).bezierCurveTo(1, 1, 1, 1, 1, 1);
    }

    @Test
    public void clearRect() {
        doCallRealMethod().when(delegateContext2D).clearRect(anyInt(), anyInt(), anyInt(), anyInt());
        delegateContext2D.clearRect(1, 1, 1, 1);
        verify(context, times(1)).clearRect(1, 1, 1, 1);
    }

    @Test
    public void clip() {
        doCallRealMethod().when(delegateContext2D).clip();
        delegateContext2D.clip();
        verify(context, times(1)).clip();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void clip2() {
        final Path2D path = null;
        doCallRealMethod().when(delegateContext2D).clip(path);
        delegateContext2D.clip(path);
    }

    @Test
    public void fill() {
        doCallRealMethod().when(delegateContext2D).fill();
        delegateContext2D.fill();
        verify(context, times(1)).fill();
    }

    @Test
    public void stroke() {
        doCallRealMethod().when(delegateContext2D).stroke();
        delegateContext2D.stroke();
        verify(context, times(1)).stroke();
    }

    @Test
    public void fillRect() {
        doCallRealMethod().when(delegateContext2D).fillRect(anyInt(), anyInt(), anyInt(), anyInt());
        delegateContext2D.fillRect(1, 1, 1, 1);
        verify(context, times(1)).fillRect(1, 1, 1, 1);
    }

    @Test
    public void fillText() {
        doCallRealMethod().when(delegateContext2D).fillText(anyString(), anyInt(), anyInt());
        delegateContext2D.fillText("text", 1, 1);
        verify(context, times(1)).fillText("text", 1, 1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void fillTextWithGradient() {
        doCallRealMethod().when(delegateContext2D).fillTextWithGradient(anyString(),
                                                                        anyInt(),
                                                                        anyInt(),
                                                                        anyInt(),
                                                                        anyInt(),
                                                                        anyInt(),
                                                                        anyInt(),
                                                                        anyString());
        delegateContext2D.fillTextWithGradient("text", 1, 1, 1, 1, 1, 1, "black");
    }

    @Test
    public void fillText1() {
        doCallRealMethod().when(delegateContext2D).fillText(anyString(), anyInt(), anyInt());
        delegateContext2D.fillText("text", 1, 1);
        verify(context, times(1)).fillText("text", 1, 1);
    }

    @Test
    public void setFillColor() {
        doCallRealMethod().when(delegateContext2D).setFillColor(anyString());
        delegateContext2D.setFillColor("black");
        verify(context, times(1)).setFillStyle("black");
    }

    @Test
    public void rect() {
        doCallRealMethod().when(delegateContext2D).rect(anyInt(), anyInt(), anyInt(), anyInt());
        delegateContext2D.rect(1, 1, 1, 1);
        verify(context, times(1)).rect(1, 1, 1, 1);
    }

    @Test
    public void rotate() {
        doCallRealMethod().when(delegateContext2D).rotate(anyInt());
        delegateContext2D.rotate(1);
        verify(context, times(1)).rotate(1);
    }

    @Test
    public void scale() {
        doCallRealMethod().when(delegateContext2D).scale(anyInt(), anyInt());
        delegateContext2D.scale(1, 1);
        verify(context, times(1)).scale(1, 1);
    }

    @Test
    public void setStrokeColor() {
        doCallRealMethod().when(delegateContext2D).setStrokeColor(anyString());
        delegateContext2D.setStrokeColor("black");
        verify(context, times(1)).setStrokeStyle("black");
    }

    @Test
    public void setStrokeWidth() {
        doCallRealMethod().when(delegateContext2D).setStrokeWidth(anyInt());
        delegateContext2D.setStrokeWidth(1);
        verify(context, times(1)).setLineWidth(1);
    }

    @Test
    public void setFillGradientLinear() {
        doCallRealMethod().when(delegateContext2D).setFillGradient(any(LinearGradient.class));
        doCallRealMethod().when(delegateContext2D).setFillColor(anyString());
        delegateContext2D.setFillGradient(new LinearGradient(linearGradientJSO));
        verify(context, times(1)).setFillStyle(null);
    }

    @Test
    public void setFillGradientRadial() {
        doCallRealMethod().when(delegateContext2D).setFillGradient(any(RadialGradient.class));
        doCallRealMethod().when(delegateContext2D).setFillColor(anyString());
        delegateContext2D.setFillGradient(new RadialGradient(radialGradientJSO));
        verify(context, times(1)).setFillStyle(null);
    }

    @Test
    public void setFillGradientPattern() {
        doCallRealMethod().when(delegateContext2D).setFillGradient(any(PatternGradient.class));
        doCallRealMethod().when(delegateContext2D).setFillColor(anyString());
        delegateContext2D.setFillGradient(new PatternGradient(patternGradientJSO));
        verify(context, times(1)).setFillStyle(null);
    }

    @Test
    public void transform() {
        doCallRealMethod().when(delegateContext2D).transform(anyInt(),
                                                             anyInt(),
                                                             anyInt(),
                                                             anyInt(),
                                                             anyInt(),
                                                             anyInt());
        delegateContext2D.transform(1, 1, 1, 1, 1, 1);
        verify(context, times(1)).transform(1, 1, 1, 1, 1, 1);
    }

    @Test
    public void transform1() {
        doCallRealMethod().when(delegateContext2D).transform(any(Transform.class));
        doCallRealMethod().when(delegateContext2D).transform(anyInt(),
                                                             anyInt(),
                                                             anyInt(),
                                                             anyInt(),
                                                             anyInt(),
                                                             anyInt());
        delegateContext2D.transform(Transform.makeFromArray(new double[]{1, 1, 1, 1, 1, 1}));
        verify(context, times(1)).transform(1, 1, 1, 1, 1, 1);
    }

    @Test
    public void setTransform() {
        doCallRealMethod().when(delegateContext2D).setTransform(any(Transform.class));
        doCallRealMethod().when(delegateContext2D).setTransform(anyInt(),
                                                                anyInt(),
                                                                anyInt(),
                                                                anyInt(),
                                                                anyInt(),
                                                                anyInt());
        delegateContext2D.setTransform(Transform.makeFromArray(new double[]{1, 1, 1, 1, 1, 1}));
        verify(context, times(1)).setTransform(1, 1, 1, 1, 1, 1);
    }

    @Test
    public void setTransform1() {
        doCallRealMethod().when(delegateContext2D).setTransform(anyInt(),
                                                                anyInt(),
                                                                anyInt(),
                                                                anyInt(),
                                                                anyInt(),
                                                                anyInt());
        delegateContext2D.setTransform(1, 1, 1, 1, 1, 1);
        verify(context, times(1)).setTransform(1, 1, 1, 1, 1, 1);
    }

    @Test
    public void setToIdentityTransform() {
        doCallRealMethod().when(delegateContext2D).setToIdentityTransform();
        doCallRealMethod().when(delegateContext2D).setTransform(anyInt(),
                                                                anyInt(),
                                                                anyInt(),
                                                                anyInt(),
                                                                anyInt(),
                                                                anyInt());
        delegateContext2D.setToIdentityTransform();
        verify(context, times(1)).setTransform(1, 0, 0, 1, 0, 0);
    }

    @Test
    public void setTextFont() {
        doCallRealMethod().when(delegateContext2D).setTextFont(anyString());
        delegateContext2D.setTextFont("arial");
        verify(context, times(1)).setFont("arial");
    }

    @Test
    public void setTextBaseline() {
        doCallRealMethod().when(delegateContext2D).setTextBaseline(TextBaseLine.BOTTOM);
        delegateContext2D.setTextBaseline(TextBaseLine.BOTTOM);
        verify(context, times(1)).setTextBaseline(TextBaseLine.BOTTOM.getValue());
    }

    @Test
    public void setTextAlign() {
        doCallRealMethod().when(delegateContext2D).setTextAlign(TextAlign.LEFT);
        delegateContext2D.setTextAlign(TextAlign.LEFT);
        verify(context, times(1)).setTextAlign(TextAlign.LEFT.getValue());
    }

    @Test
    public void strokeText() {
        doCallRealMethod().when(delegateContext2D).strokeText(anyString(), anyInt(), anyInt());
        delegateContext2D.strokeText("text", 1, 1);
        verify(context, times(1)).strokeText("text", 1, 1);
    }

    @Test
    public void setGlobalAlpha() {
        doCallRealMethod().when(delegateContext2D).setGlobalAlpha(anyInt());
        delegateContext2D.setGlobalAlpha(1);
        verify(context, times(1)).setGlobalAlpha(1);
    }

    @Test
    public void translate() {
        doCallRealMethod().when(delegateContext2D).translate(anyInt(), anyInt());
        delegateContext2D.translate(1, 1);
        verify(context, times(1)).translate(1, 1);
    }

    @Test
    public void setShadow() {
        doCallRealMethod().when(delegateContext2D).setShadow(null);
        delegateContext2D.setShadow(null);
        verify(context).setShadowColor(Mockito.anyString());
        verify(context).setShadowOffsetX(Mockito.anyInt());
        verify(context).setShadowOffsetY(Mockito.anyInt());
        verify(context).setShadowBlur(Mockito.anyInt());

        delegateContext2D.setShadow(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void isSupported() {
        doCallRealMethod().when(delegateContext2D).isSupported(anyString());
        delegateContext2D.isSupported("feature");
    }

    @Test
    public void isPointInPath() {
        doCallRealMethod().when(delegateContext2D).isPointInPath(anyInt(), anyInt());
        delegateContext2D.isPointInPath(1, 1);
        verify(context, times(1)).isPointInPath(1, 1);
    }

    @Test
    public void getImageData() {
        doCallRealMethod().when(delegateContext2D).getImageData(anyInt(), anyInt(), anyInt(), anyInt());
        delegateContext2D.getImageData(1, 1, 1, 1);
        verify(context, times(1)).getImageData(1, 1, 1, 1);
    }

    @Test
    public void createImageData() {
        doCallRealMethod().when(delegateContext2D).createImageData(null);
        delegateContext2D.createImageData(null);
        verify(context, times(1)).createImageData(null);
    }

    @Test
    public void createImageData1() {
        doCallRealMethod().when(delegateContext2D).createImageData(anyInt(), anyInt());
        delegateContext2D.createImageData(1, 1);
        verify(context, times(1)).createImageData(1, 1);
    }

    @Test
    public void putImageData() {
        doCallRealMethod().when(delegateContext2D).putImageData(null, 1, 1);
        delegateContext2D.putImageData(null, 1, 1);
        verify(context, times(1)).putImageData(null, 1, 1);
    }

    @Test
    public void putImageData1() {
        doCallRealMethod().when(delegateContext2D).putImageData(null, 1, 1, 1, 1, 1, 1);
        delegateContext2D.putImageData(null, 1, 1, 1, 1, 1, 1);
        verify(context, times(1)).putImageData(null, 1, 1, 1, 1, 1, 1);
    }

    @Test
    public void measureText() {
        doCallRealMethod().when(delegateContext2D).measureText(anyString());
        delegateContext2D.measureText("text");
        verify(context, times(1)).measureText("text");
    }

    @Test
    public void resetClip() {
        doCallRealMethod().when(delegateContext2D).resetClip();
        delegateContext2D.resetClip();
        verify(context, times(1)).resetClip();
    }

    @Test
    public void setMiterLimit() {
        doCallRealMethod().when(delegateContext2D).setMiterLimit(anyInt());
        delegateContext2D.setMiterLimit(1);
        verify(context, times(1)).setMiterLimit(1);
    }

    @Test
    public void setLineDash() {
        doCallRealMethod().when(delegateContext2D).setLineDash(any(DashArray.class));
        delegateContext2D.setLineDash(new DashArray(new double[]{1, 1}));
        verify(context, times(1)).setLineDash(new double[]{1, 1});
    }

    @Test
    public void setLineDashOffset() {
        doCallRealMethod().when(delegateContext2D).setLineDashOffset(anyInt());
        delegateContext2D.setLineDashOffset(1);
        verify(context, times(1)).setLineDashOffset(1);
    }

    @Test
    public void getBackingStorePixelRatio() {
        doCallRealMethod().when(delegateContext2D).getBackingStorePixelRatio();
        assertEquals(delegateContext2D.getBackingStorePixelRatio(), 1, 0);
    }

    @Test
    public void drawImage() {
        doCallRealMethod().when(delegateContext2D).drawImage(element, 1, 1);
        delegateContext2D.drawImage(element, 1, 1);
        verify(context, times(1)).drawImage(htmlElement, 1, 1);
    }

    @Test
    public void drawImage2() {
        doCallRealMethod().when(delegateContext2D).drawImage(element, 1, 1, 1, 1);
        delegateContext2D.drawImage(element, 1, 1, 1, 1);
        verify(context, times(1)).drawImage(htmlElement, 1, 1, 1, 1);
    }

    @Test
    public void drawImage3() {
        doCallRealMethod().when(delegateContext2D).drawImage(element, 1, 1, 1, 1, 1, 1, 1, 1);
        delegateContext2D.drawImage(element, 1, 1, 1, 1, 1, 1, 1, 1);
        verify(context, times(1)).drawImage(htmlElement, 1, 1, 1, 1, 1, 1, 1, 1);
    }
}