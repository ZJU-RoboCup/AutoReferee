/*
 * *********************************************************
 * Copyright (c) 2009 DHBW Mannheim - Tigers Mannheim
 * Project: tigers-centralSoftware
 * Date: 29.08.2009
 * Authors: Bernhard Perun <bernhard.perun@googlemail.com>
 * *********************************************************
 */
package edu.tigers.sumatra.util;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.tigers.sumatra.components.BetterScrollPane;
import edu.tigers.sumatra.util.GlobalShortcuts.EShortcut;
import net.miginfocom.swing.MigLayout;


/**
 */
public class ShortcutsDialog extends JDialog
{
	// --------------------------------------------------------------------------
	// --- instance-variables ---------------------------------------------------
	// --------------------------------------------------------------------------
	private static final long serialVersionUID = 3461893941869192656L;
	
	
	// --------------------------------------------------------------------------
	// --- constructor(s) -------------------------------------------------------
	// --------------------------------------------------------------------------
	/**
	 * Initializes about - dialog.
	 */
	public ShortcutsDialog()
	{
		// --- window configuration ---
		this.setSize(320, 400);
		setResizable(false);
		setTitle("Shortcuts");
		setModal(true);
		
		// --- alignment: center on screen ---
		Toolkit tk = Toolkit.getDefaultToolkit();
		Dimension screenDimension = tk.getScreenSize();
		this.setLocation((int) (screenDimension.getWidth() - getWidth()) / 2,
				(int) (screenDimension.getHeight() - getHeight()) / 2);
				
		JPanel scPanel = new JPanel(new MigLayout("fill", ""));
		for (EShortcut eShortcut : Arrays.stream(EShortcut.values()).sorted(Comparator.comparing(Enum::name))
				.collect(Collectors.toList()))
		{
			JLabel lblId = new JLabel(eShortcut.name());
			JLabel lblDesc = new JLabel(eShortcut.getDesc());
			scPanel.add(lblId);
			scPanel.add(lblDesc, "wrap");
		}
		BetterScrollPane scrollPane = new BetterScrollPane(scPanel);
		add(scrollPane);
	}
}
