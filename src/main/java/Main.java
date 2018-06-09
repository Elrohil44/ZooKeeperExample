import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

public class Main implements Watcher{
    public static String ZNODE_TO_WATCH = "/znode_testowy";
    private ZooKeeper zk;
    private String executable;
    private Process process;
    private ZNodeWatcher watcher;

    public Main(String connectString, String executable) {
        this.executable = executable;
        try {
            zk = new ZooKeeper(connectString, 3000, this);
            watcher = new ZNodeWatcher(zk);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) throws InterruptedException, KeeperException {
        String exec="", connectString="";

        try {
            exec = args[0];
            connectString = String.join(",",
                    Arrays.copyOfRange(args, 1, args.length));
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Too few arguments");
            System.out.println("Expected arguments: EXECUTABLE [HOST:PORT]+");
            System.exit(1);
        }

        new Main(connectString, exec).run();
    }

    private void run() throws InterruptedException {
        zk.exists(ZNODE_TO_WATCH, true,
                watcher, null);

        Scanner scanner = new Scanner(new BufferedInputStream(System.in));
        while (scanner.hasNextLine()) {
            switch (scanner.nextLine()) {
                case "quit":
                    zk.close();
                    return;
                case "tree":
                    printTree("/", 0);
                    break;
                default:
                    System.out.println("Command not recognized!");
            }
        }
    }

    private void printTree(String node, int indent) {
        String[] path = node.split("/");
        System.out.println(getIndent(indent) +
                (path.length > 0 ? path[path.length - 1] : "") + "/");
        try {
            zk.getChildren(node, false)
                    .forEach(child ->
                            printTree((node.length() == 1 ? "" : node)
                                    + "/" + child, indent + 1));
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private String getIndent(int indent) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            builder.append("|  ");
        }
        builder.append("+--");
        return builder.toString();
    }

    @Override
    public void process(WatchedEvent event) {
        System.out.println(event);
        if (event.getPath().equals(ZNODE_TO_WATCH)) {
            switch (event.getType()) {
                case NodeCreated:
                    zk.exists(ZNODE_TO_WATCH, true,
                            watcher, null);
                    try {
                        process = Runtime.getRuntime().exec(executable);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case NodeDeleted:
                    if (process != null && process.isAlive()) {
                        process.destroy();
                    }
                    break;
                default:
            }
            try {
                zk.exists(ZNODE_TO_WATCH, true);
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
