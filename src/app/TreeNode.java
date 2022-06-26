package app;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {

    public static  Integer parentId = -1;
    public static Integer rootId = -1;
    public static List<Integer> childrenIds = new ArrayList<>();
    //maybe tree lock?

    public static void addChildren(List<Integer> childrenToBeAdded)
    {
        childrenIds.addAll(childrenToBeAdded);
    }

    public static void resetNode()
    {
        parentId = -1;
        rootId = -1;
        childrenIds.clear();
    }

    public static void setNode(Integer root, Integer parent)
    {
        TreeNode.rootId = root;
        TreeNode.parentId = parent;
    }
}
