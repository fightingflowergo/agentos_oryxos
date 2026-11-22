package io.oryxos.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;

/** Minimal CLI entry point; runtime commands are added in later implementation stages. */
@Command(
        name = "oryxos",
        mixinStandardHelpOptions = true,
        versionProvider = OryxOsCli.VersionProvider.class,
        description = "OryxOS - Enterprise Agent Harness OS")
public class OryxOsCli implements Runnable {

    public static void main(String[] args) {
        System.exit(new CommandLine(new OryxOsCli()).execute(args));
    }

    @Override
    public void run() {
        for (String version : new VersionProvider().getVersion()) {
            System.out.println(version);
        }
    }

    /** Reads the version written into this module's JAR manifest by Maven. */
    public static class VersionProvider implements IVersionProvider {

        @Override
        public String[] getVersion() {
            String version = OryxOsCli.class.getPackage().getImplementationVersion();
            return new String[] {"oryxos " + (version == null ? "development" : version)};
        }
    }
}
