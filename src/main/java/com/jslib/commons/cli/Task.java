package com.jslib.commons.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystems;

import js.io.IConsole;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true)
public abstract class Task implements Runnable {
	@Option(names = "--time", description = "Measure execution time. Default: ${DEFAULT-VALUE}.", defaultValue = "false")
	private boolean time;
	@Option(names = { "-x", "--exception" }, description = "Print stack trace on exception.")
	private boolean stacktrace;

	protected IConsole console;
	protected Config config;
	protected FilesUtil files;
	protected WebsUtil webs;

	protected Task() {
		this.console = new Console();
		this.files = new FilesUtil(FileSystems.getDefault(), this.console);
		this.webs = new WebsUtil(this.console);
	}

	protected Task(Task parent) {
		this.console = parent.console;
		this.config = parent.config;
		this.files = parent.files;
		this.webs = parent.webs;
	}

	public void setConsole(IConsole console) {
		this.console = console;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public void setFiles(FilesUtil files) {
		this.files = files;
	}

	public void setWebs(WebsUtil webs) {
		this.webs = webs;
	}

	@Override
	public void run() {
		long start = System.nanoTime();
		ExitCode exitCode = ExitCode.SUCCESS;
		try {
			exitCode = exec();
		} catch (IOException e) {
			handleException(e);
			exitCode = ExitCode.SYSTEM_FAIL;
		} catch (Throwable t) {
			handleException(t);
			exitCode = ExitCode.APPLICATION_FAIL;
		}
		if (time) {
			console.print("Processing time: %.04f msec.", (System.nanoTime() - start) / 1000000.0);
		}
		System.exit(exitCode.ordinal());
	}

	protected abstract ExitCode exec() throws Exception;

	private void handleException(Throwable t) {
		if (stacktrace) {
			StringWriter buffer = new StringWriter();
			t.printStackTrace(new PrintWriter(buffer));
			console.error(buffer);
		} else {
			StringBuilder message = new StringBuilder();
			message.append(t.getClass().getSimpleName());
			if (t.getMessage() != null) {
				message.append(": ");
				message.append(t.getMessage());
			}
			console.error(message.toString());
		}
	}
}
