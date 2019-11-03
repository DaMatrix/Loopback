/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2019-2019 DaPorkchop_ and contributors
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it. Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.loopback;

import net.daporkchop.lib.hash.util.Digest;
import net.daporkchop.lib.logging.LogAmount;
import net.daporkchop.lib.logging.Logging;
import net.daporkchop.loopback.client.Client;
import net.daporkchop.loopback.server.Server;
import net.daporkchop.loopback.util.Addr;
import net.daporkchop.loopback.util.Endpoint;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.daporkchop.loopback.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public final class Loopback {
    private static void displayHelp() {
        Logging.logger.info("Usage:")
                .info("  java -jar loopback.jar client <password> <address>:<port>")
                .info("or")
                .info("  java -jar loopback.jar server <password> <port>");
        System.exit(0);
    }

    public static void main(String... args) {
        Logging.logger.enableANSI().redirectStdOut()/*.setLogAmount(LogAmount.DEBUG)*/;
        if (args.length != 3) displayHelp();

        Endpoint endpoint = null;

        try {
            byte[] hash = Digest.SHA_256.hash(args[1].getBytes(StandardCharsets.UTF_8)).getHash();
            switch (args[0]) {
                case "client": {
                    Matcher matcher = Pattern.compile("^([^:]+):([0-9]{1,4}|[0-5][0-9]{4}|6[0-5]{2}[0-3][0-5])$").matcher(args[2]);
                    if (!matcher.find()) throw new IllegalArgumentException(String.format("Invalid address:port (\"%s\")", args[2]));
                    Addr addr = new Addr(matcher.group(1), Integer.parseInt(matcher.group(2)));

                    if (true) {
                        Test.startClient(addr);
                        System.exit(0);
                    }

                    endpoint = new Client(hash, addr);
                }
                break;
                case "server":
                    if (true) {
                        Test.startServer(Integer.parseInt(args[2]));
                        System.exit(0);
                    }

                    endpoint = new Server(hash, Integer.parseInt(args[2]));
                    break;
                default:
                    throw new IllegalArgumentException(String.format("Invalid mode (\"%s\")", args[0]));
            }
        } catch (RuntimeException e) {
            Logging.logger.error(e.getClass().getCanonicalName());
            if (e.getMessage() != null) Logging.logger.error(e.getMessage());
            displayHelp();
        }

        try {
            Logging.logger.info("Starting...");
            endpoint.start();

            try (Scanner scanner = new Scanner(System.in)) {
                Logging.logger.success("Started! Type \"stop\" to stop.");
                while (!endpoint.handleCommand(scanner.nextLine())) ;
                Logging.logger.info("Stopping...");
            }
            Logging.logger.success("Stopped!");
        } finally {
            try {
                endpoint.close().syncUninterruptibly();
            } finally {
                GROUP.shutdownGracefully();
            }
        }
    }
}
