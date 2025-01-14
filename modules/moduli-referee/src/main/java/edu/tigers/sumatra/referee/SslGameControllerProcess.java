/*
 * Copyright (c) 2009 - 2020, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.referee;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;


/**
 * A wrapper around the ssl-game-controller executable
 */
@Log4j2
public class SslGameControllerProcess implements Runnable
{
	private static final String BINARY_NAME = "ssl-game-controller";
	private static final Path TEMP_DIR = Paths.get("temp");
	private static final File BINARY_FILE = TEMP_DIR.resolve(BINARY_NAME).toFile();

	@Getter
	private final int gcUiPort;
	private final String publishAddress;
	private final String timeAcquisitionMode;

	private Process process = null;


	public SslGameControllerProcess(int gcUiPort, String publishAddress, String timeAcquisitionMode)
	{
		this.gcUiPort = gcUiPort;
		this.publishAddress = publishAddress;
		this.timeAcquisitionMode = timeAcquisitionMode;
	}


	@Override
	public void run()
	{
		Thread.currentThread().setName(BINARY_NAME);

		if (!setupBinary())
		{
			return;
		}

		try
		{
			log.debug("Starting with: {} {} {}", gcUiPort, timeAcquisitionMode, publishAddress);
			List<String> command = new ArrayList<>();
			command.add(BINARY_FILE.getAbsolutePath());
			command.add("-address");
			command.add(":" + gcUiPort);
			command.add("-timeAcquisitionMode");
			command.add(timeAcquisitionMode);
			if(!publishAddress.isBlank())
			{
				command.add("-publishAddress");
				command.add(publishAddress);
			}

			ProcessBuilder builder = new ProcessBuilder(command);
			builder.redirectErrorStream(true);
			builder.directory(Paths.get("").toAbsolutePath().toFile());
			Process gcProcess = builder.start();
			process = gcProcess;
			log.debug("game-controller process started");

			Scanner s = new Scanner(gcProcess.getInputStream());
			inputLoop(s);
			s.close();
		} catch (IOException e)
		{
			if (!"Stream closed".equals(e.getMessage()))
			{
				log.warn("Could not execute ssl-game-controller", e);
			}
		}
		// thread safe local variable
		Process p = process;
		if (p != null && !p.isAlive() && p.exitValue() != 0)
		{
			log.warn("game-controller has returned a non-zero exit code: {}", p.exitValue());
		}
		log.debug("game-controller process thread finished");
	}


	private boolean setupBinary()
	{
		if (BINARY_FILE.exists() && !BINARY_FILE.delete())
		{
			log.warn("Could not delete existing binary file: {}", BINARY_FILE);
			return false;
		}
		File tmpDir = TEMP_DIR.toFile();
		if (tmpDir.mkdirs())
		{
			log.debug("Temp dir created: {}", tmpDir);
			tmpDir.deleteOnExit();
		}
		if (!writeResourceToFile(BINARY_NAME, BINARY_FILE))
		{
			return false;
		}
		BINARY_FILE.deleteOnExit();
		if (!BINARY_FILE.canExecute() && !BINARY_FILE.setExecutable(true))
		{
			log.warn("Binary is not executable and could not be made executable.");
			return false;
		}
		return true;
	}


	private static boolean writeResourceToFile(String resourcePath, File targetFile)
	{
		try
		{
			InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(resourcePath);
			if (in == null)
			{
				log.warn("Could not find {} in classpath", resourcePath);
				return false;
			}

			try (FileOutputStream out = new FileOutputStream(targetFile))
			{
				IOUtils.copy(in, out);
			}
			return true;
		} catch (IOException e)
		{
			log.warn("Could not copy binary to temporary file", e);
		}
		return false;
	}


	private void inputLoop(final Scanner s)
	{
		while (s.hasNextLine())
		{
			String line = s.nextLine();
			if (line != null)
			{
				log.debug("GC: {}", line);
			}
		}
	}


	public void stop()
	{
		Process gcProcess = process;
		process = null;
		if (gcProcess == null)
		{
			return;
		}

		gcProcess.destroy();
		try
		{
			if (!gcProcess.waitFor(1, TimeUnit.SECONDS))
			{
				log.warn("Process could not be stopped and must be killed");
				gcProcess.destroyForcibly();
			}
		} catch (InterruptedException e)
		{
			log.warn("Interrupted while waiting for the process to exit");
			Thread.currentThread().interrupt();
		}
	}
}
