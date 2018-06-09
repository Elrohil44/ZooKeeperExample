import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.List;

public class ZNodeWatcher implements org.apache.zookeeper.Watcher, AsyncCallback.StatCallback {
    private ZooKeeper zk;

    public ZNodeWatcher(ZooKeeper zk) {
        this.zk = zk;
    }

    private int getChildrenCount(String node) throws KeeperException, InterruptedException {
        int count = 0;
        List<String> children = zk.getChildren(node, this);
        count += children.size();
        for (String child: children) {
            String childPath = String.format("%s/%s", node, child);
            int countChild = getChildrenCount(childPath);
            count += countChild;
        }
        return count;
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeChildrenChanged) {
            System.out.println(event);
            System.out.print("Liczba potomków:\t");
            try {
                System.out.println(getChildrenCount(Main.ZNODE_TO_WATCH));
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        switch (rc) {
            case Code.Ok:
                break;
            default:
                return;
        }

        try {
            getChildrenCount(Main.ZNODE_TO_WATCH);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
