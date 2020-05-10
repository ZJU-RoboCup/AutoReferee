/*
 * Copyright (c) 2009 - 2020, DHBW Mannheim - TIGERs Mannheim
 */
package edu.tigers.sumatra.view.referee;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.io.IOException;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.apache.commons.lang.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.tigers.sumatra.SslGcRefereeMessage;
import edu.tigers.sumatra.SslGcRefereeMessage.Referee.Command;
import edu.tigers.sumatra.model.SumatraModel;
import edu.tigers.sumatra.referee.Referee;
import net.miginfocom.swing.MigLayout;


/**
 * Incoming referee messages are displayed here.
 */
public class ShowRefereeMsgPanel extends JPanel
{
	private static final long serialVersionUID = -508393753936993622L;
	private static final Logger log = LogManager.getLogger(ShowRefereeMsgPanel.class.getName());
	private static final int MAX_COMMANDS = 50;
	private static final String SPAN_2 = "span 2";

	private final JButton openControllerButton;
	private final JButton toggleGameController;
	private final DefaultListModel<Command> listModel = new DefaultListModel<>();
	private Command lastCmd = null;
	private final JLabel time;
	private final JLabel goals;
	private final JLabel stage;
	private final JLabel command;

	private final DecimalFormat df2 = new DecimalFormat("00");


	public ShowRefereeMsgPanel()
	{
		setLayout(new MigLayout("wrap 2", "[fill]10[fill]"));

		openControllerButton = new JButton("Open SSL Game Controller UI");
		openControllerButton.addActionListener(a -> open());
		add(openControllerButton, SPAN_2);

		toggleGameController = new JButton("Start Game Controller");
		toggleGameController.addActionListener(a -> toggleGameController());
		add(toggleGameController, SPAN_2);

		add(new JLabel("Stage:"));
		stage = new JLabel();
		stage.setFont(stage.getFont().deriveFont(Font.BOLD));
		add(stage);

		add(new JLabel("Last Command:"));
		command = new JLabel();
		command.setFont(stage.getFont().deriveFont(Font.BOLD));
		add(command);

		// Goals
		add(new JLabel("Goals:"));
		goals = new JLabel();
		goals.setFont(goals.getFont().deriveFont(Font.BOLD));
		add(goals);

		// Time
		add(new JLabel("Time:"));
		time = new JLabel();
		time.setFont(time.getFont().deriveFont(Font.BOLD));
		add(time);

		// Commands
		add(new JLabel("All Commands: "), "wrap");
		final JList<?> commandList = new JList<>(listModel);
		commandList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		commandList.setLayoutOrientation(JList.VERTICAL);
		commandList.setVisibleRowCount(-1);

		JScrollPane listScroller = new JScrollPane(commandList);
		listScroller.setPreferredSize(new Dimension(commandList.getMaximumSize().width, this.getPreferredSize().height));
		add(listScroller, SPAN_2);
	}


	private void open()
	{
		String gameControllerAddress = "http://localhost:"
				+ SumatraModel.getInstance().getModule(Referee.class).getGameControllerUiPort();
		try
		{
			if (SystemUtils.IS_OS_UNIX
					&& Runtime.getRuntime().exec(new String[] { "which", "xdg-open" }).getInputStream().read() != -1)
			{
				// Desktop#browse is not well supported with Linux, so try xdg-open first
				Runtime.getRuntime().exec(new String[] { "xdg-open", gameControllerAddress });
				return;
			}
			if (Desktop.isDesktopSupported())
			{
				Desktop.getDesktop().browse(URI.create(gameControllerAddress));
			} else
			{
				log.warn("Opening web browser is not supported.");
			}
		} catch (IOException e)
		{
			log.warn("Could not execute command to open browser", e);
		}
	}


	private void toggleGameController()
	{
		final Referee referee = SumatraModel.getInstance().getModule(Referee.class);
		if (referee.isInternalGameControllerUsed())
		{
			referee.stopGameController();
		} else
		{
			referee.startGameController();
		}
		updateToggleGameControllerButton();
	}


	/**
	 * @param msg
	 */
	public void update(final SslGcRefereeMessage.Referee msg)
	{
		// Information on Top
		EventQueue.invokeLater(() -> {
			// Goals
			goals.setText(msg.getYellow().getScore() + " (Y) : (B) " + msg.getBlue().getScore());
			// Time
			final long min = TimeUnit.MICROSECONDS.toMinutes(msg.getStageTimeLeft());
			final long sec = TimeUnit.MICROSECONDS.toSeconds(msg.getStageTimeLeft()) - (60 * min);
			time.setText(df2.format(min) + ":" + df2.format(sec));
			stage.setText(msg.getStage().name());
			command.setText(msg.getCommand().name());
		});

		// Command History
		EventQueue.invokeLater(() -> {
			if (!msg.getCommand().equals(lastCmd))
			{
				lastCmd = msg.getCommand();
				if (listModel.size() > MAX_COMMANDS)
				{
					listModel.removeElementAt(0);
				}
				listModel.add(0, msg.getCommand());
			}
		});
	}


	@Override
	public void setEnabled(final boolean enable)
	{
		super.setEnabled(enable);
		EventQueue.invokeLater(() -> openControllerButton.setEnabled(enable));
		EventQueue.invokeLater(() -> toggleGameController.setEnabled(enable));

		if (enable)
		{
			updateToggleGameControllerButton();
		}
	}


	private void updateToggleGameControllerButton()
	{
		final Referee referee = SumatraModel.getInstance().getModule(Referee.class);
		if (referee.isInternalGameControllerUsed())
		{
			toggleGameController.setText("Stop Game Controller");
		} else
		{
			toggleGameController.setText("Start Game Controller");
		}
	}
}
