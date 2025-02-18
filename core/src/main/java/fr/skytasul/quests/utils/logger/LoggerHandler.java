package fr.skytasul.quests.utils.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import fr.skytasul.quests.BeautyQuests;
import fr.skytasul.quests.utils.Utils;

public class LoggerHandler extends Handler implements ILoggerHandler {

	private final Date launchDate = new Date();
	
	private File file;
	private PrintWriter stream;
	
	private SimpleDateFormat format = new SimpleDateFormat("[HH:mm:ss] ");
	private Date date = new Date(System.currentTimeMillis());
	
	private BukkitRunnable run;
	private boolean something = false;
	
	private List<String> errors = new ArrayList<>();
	
	public LoggerHandler(Plugin plugin) throws IOException {
		super.setFormatter(new Formatter() {
			@Override
			public String format(LogRecord record) {
				return super.formatMessage(record);
			}
		});
		
		file = new File(plugin.getDataFolder(), "latest.log");
		if (file.exists()) {
			Files.move(file.toPath(), new File(plugin.getDataFolder(), "latest.log_old").toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		Files.createFile(file.toPath());
		stream = new PrintWriter(new FileWriter(file));
		write("---- BEAUTYQUESTS LOGGER - OPENED " + launchDate.toString() + " ----");
	}
	
	public boolean isEnabled() {
		return stream != null;
	}
	
	@Override
	public void publish(LogRecord logRecord) {
		try {
			if (logRecord != null) {
				write("[" + (logRecord.getLevel() == null ? "NONE" : logRecord.getLevel().getName()) + "]: " + getFormatter().format(logRecord));
				if (logRecord.getThrown() != null) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					logRecord.getThrown().printStackTrace(pw);
					pw.close();
					String throwable = sw.toString();
					int index = errors.indexOf(throwable);
					if (index == -1) {
						index = errors.size();
						write("[ERROR] new #" + index + ": " + throwable);
						errors.add(throwable);
					}else write("[ERROR] existing #" + index);
				}
			}
		}catch (Exception ex) {
			reportError("An error occurred while trying to publish a log record.", ex, ErrorManager.GENERIC_FAILURE);
		}
	}
	
	@Override
	public void write(String msg){
		if (!isEnabled()) return;
		date.setTime(System.currentTimeMillis());
		stream.println(format.format(date) + msg);
		something = true;
	}
	
	@Override
	public void close() {
		if (stream == null) return;
		Date endDate = new Date();
		write("Logger was open during " + Utils.millisToHumanString(endDate.getTime() - launchDate.getTime()));
		write("---- BEAUTYQUESTS LOGGER - CLOSED " + endDate.toString() + " ----");
		if (!isEnabled()) return;
		if (run != null) {
			run.cancel();
			run = null;
		}
		stream.close();
		stream = null;
	}
	
	public void launchFlushTimer() {
		if (run != null) return;
		if (!isEnabled()) return;
		run = new BukkitRunnable() {
			@Override
			public void run() {
				if (!something) return;
				stream.flush();
				something = false;
			}
		};
		run.runTaskTimerAsynchronously(BeautyQuests.getInstance(), 2L, 50L);
	}

	@Override
	public void flush() {
		run.run();
	}
	
}
