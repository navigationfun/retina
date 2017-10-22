// code by jph
package ch.ethz.idsc.retina.gui.gokart;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Optional;

import javax.swing.JCheckBox;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import ch.ethz.idsc.retina.dev.linmot.LinmotGetEvent;
import ch.ethz.idsc.retina.dev.linmot.LinmotPutEvent;
import ch.ethz.idsc.retina.dev.linmot.LinmotPutHelper;
import ch.ethz.idsc.retina.util.data.Word;
import ch.ethz.idsc.retina.util.gui.SliderExt;
import ch.ethz.idsc.retina.util.gui.SpinnerLabel;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.img.ColorDataGradients;
import ch.ethz.idsc.tensor.img.ColorFormat;
import ch.ethz.idsc.tensor.sca.Round;

class LinmotComponent extends AutoboxTestingComponent<LinmotGetEvent, LinmotPutEvent> {
  public final LinmotInitButton linmotInitButton = new LinmotInitButton();
  private final SpinnerLabel<Word> spinnerLabelCtrl = new SpinnerLabel<>();
  private final SpinnerLabel<Word> spinnerLabelHdr = new SpinnerLabel<>();
  private final SliderExt sliderExtTPos;
  private final SliderExt sliderExtMVel;
  private final SliderExt sliderExtAcc;
  private final SliderExt sliderExtDec;
  // ---
  private final JTextField jTextFieldStatusWord;
  private final JTextField jTextFieldStateVariable;
  private final JTextField jTextFieldActualPosition;
  private final JTextField jTextFieldDemandPosition;
  private final JTextField jTextFieldWindingTemp1;
  private final JTextField jTextFieldWindingTemp2;
  private final JCheckBox[] jCheckBoxStatusWord = new JCheckBox[16];

  public LinmotComponent() {
    {
      JToolBar jToolBar = createRow("Special Routines");
      jToolBar.add(linmotInitButton.getComponent());
    }
    {
      JToolBar jToolBar = createRow("control word");
      spinnerLabelCtrl.setList(LinmotPutHelper.COMMANDS);
      spinnerLabelCtrl.setValueSafe(LinmotPutHelper.CMD_OPERATION);
      spinnerLabelCtrl.addToComponent(jToolBar, new Dimension(200, 20), "");
    }
    { // command speed
      JToolBar jToolBar = createRow("motion cmd hdr");
      spinnerLabelHdr.setList(LinmotPutHelper.HEADER);
      spinnerLabelHdr.setValueSafe(LinmotPutHelper.MC_POSITION);
      spinnerLabelHdr.addToComponent(jToolBar, new Dimension(200, 20), "");
    }
    { // target pos
      JToolBar jToolBar = createRow("target pos");
      sliderExtTPos = SliderExt.wrap(new JSlider( //
          LinmotPutHelper.TARGETPOS_MIN, //
          LinmotPutHelper.TARGETPOS_MAX, //
          LinmotPutHelper.TARGETPOS_INIT));
      sliderExtTPos.addToComponent(jToolBar);
      // sliderExtF2.setValueShort(init.target_position);
    }
    { // max velocity
      JToolBar jToolBar = createRow("max velocity");
      sliderExtMVel = SliderExt.wrap(new JSlider( //
          LinmotPutHelper.MAXVELOCITY_MIN, //
          LinmotPutHelper.MAXVELOCITY_MAX, //
          LinmotPutHelper.MAXVELOCITY_INIT));
      sliderExtMVel.addToComponent(jToolBar);
    }
    { // acceleration
      JToolBar jToolBar = createRow("acceleration");
      sliderExtAcc = SliderExt.wrap(new JSlider( //
          LinmotPutHelper.ACCELERATION_MIN, //
          LinmotPutHelper.ACCELERATION_MAX, //
          LinmotPutHelper.ACCELERATION_INIT));
      sliderExtAcc.addToComponent(jToolBar);
    }
    { // deceleration
      JToolBar jToolBar = createRow("deceleration");
      sliderExtDec = SliderExt.wrap(new JSlider( //
          LinmotPutHelper.DECELERATION_MIN, //
          LinmotPutHelper.DECELERATION_MAX, //
          LinmotPutHelper.DECELERATION_INIT));
      sliderExtDec.addToComponent(jToolBar);
    }
    addSeparator();
    {
      jTextFieldStatusWord = createReading("status word");
      // ---
      for (int index = 0; index < LinmotStatusWord.TITLES.length; ++index)
        jCheckBoxStatusWord[index] = //
            createReadingCheckbox(index + " " + LinmotStatusWord.TITLES[index]);
      // ---
      jTextFieldStateVariable = createReading("state variable");
      jTextFieldActualPosition = createReading("actual pos.");
      jTextFieldDemandPosition = createReading("demand pos.");
      jTextFieldWindingTemp1 = createReading("winding temp.1");
      jTextFieldWindingTemp2 = createReading("winding temp.2");
    }
  }

  @Override
  public void getEvent(LinmotGetEvent linmotGetEvent) {
    jTextFieldStatusWord.setText(String.format("%04X", linmotGetEvent.status_word));
    for (int index = 0; index < LinmotStatusWord.TITLES.length; ++index) {
      boolean selected = (linmotGetEvent.status_word & (1 << index)) != 0;
      jCheckBoxStatusWord[index].setSelected(selected);
    }
    jTextFieldStateVariable.setText(String.format("%04X", linmotGetEvent.state_variable));
    jTextFieldActualPosition.setText("" + linmotGetEvent.actual_position);
    jTextFieldDemandPosition.setText("" + linmotGetEvent.demand_position);
    Scalar scalar = RealScalar.of(linmotGetEvent.getPositionDiscrepancyRaw());
    jTextFieldDemandPosition.setBackground(ColorFormat.toColor( //
        ColorDataGradients.TEMPERATURE.apply(LinmotGetEvent.NOMINAL_POSITION_DELTA.rescale(scalar))));
    {
      Scalar temp = linmotGetEvent.getWindingTemperature1();
      jTextFieldWindingTemp1.setText(temp.map(Round._1).toString());
      Scalar value = LinmotGetEvent.TEMPERATURE_RANGE.rescale(temp);
      Tensor vector = ColorDataGradients.THERMOMETER.apply(value);
      Color color = ColorFormat.toColor(vector);
      jTextFieldWindingTemp1.setBackground(color);
    }
    {
      Scalar temp = linmotGetEvent.getWindingTemperature2();
      jTextFieldWindingTemp2.setText(temp.map(Round._1).toString());
      Scalar value = LinmotGetEvent.TEMPERATURE_RANGE.rescale(temp);
      Tensor vector = ColorDataGradients.THERMOMETER.apply(value);
      Color color = ColorFormat.toColor(vector);
      jTextFieldWindingTemp2.setBackground(color);
    }
  }

  @Override
  public Optional<LinmotPutEvent> putEvent() {
    LinmotPutEvent linmotPutEvent = //
        new LinmotPutEvent(spinnerLabelCtrl.getValue(), spinnerLabelHdr.getValue());
    linmotPutEvent.target_position = (short) sliderExtTPos.jSlider.getValue();
    linmotPutEvent.max_velocity = (short) sliderExtMVel.jSlider.getValue();
    linmotPutEvent.acceleration = (short) sliderExtAcc.jSlider.getValue();
    linmotPutEvent.deceleration = (short) sliderExtDec.jSlider.getValue();
    return Optional.of(linmotPutEvent);
  }

  @Override
  public void putEvent(LinmotPutEvent linmotPutEvent) {
    if (linmotPutEvent.isOperational())
      sliderExtTPos.jSlider.setValue(linmotPutEvent.target_position);
    spinnerLabelCtrl.setValue(LinmotPutHelper.findControlWord(linmotPutEvent.control_word));
    spinnerLabelHdr.setValue(LinmotPutHelper.findHeaderWord(linmotPutEvent.motion_cmd_hdr));
    // sliderExtTPos.jSlider.setValue(linmotPutEvent.target_position);
    // sliderExtMVel.jSlider.setValue(linmotPutEvent.max_velocity);
    // sliderExtAcc.jSlider.setValue(linmotPutEvent.acceleration);
    // sliderExtDec.jSlider.setValue(linmotPutEvent.deceleration);
  }
}