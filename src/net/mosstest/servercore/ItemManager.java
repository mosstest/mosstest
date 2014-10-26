package net.mosstest.servercore;

import net.mosstest.scripting.MapNode;
import net.mosstest.scripting.MossItem;
import net.mosstest.scripting.MossItemBuilder;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.HashMap;

// TODO: Auto-generated Javadoc

/**
 * The Class ItemManager.
 */
public class ItemManager {

    public static final int MAX_ITEMDEF = 32000;
    public static final int BYTE_CAST_MASK = 0xFF;

    private ArrayList<MossItem> defItems = new ArrayList<>();

    private HashMap<String, MossItem> defItemsByName = new HashMap<>();



    /**
     * The unknown fallback item.
     */
    @NonNls
    private final MossItem unknownFallbackItem = new MossItemBuilder().setInvTex("item_unknown.png").setWieldTex("sys_hand.png").setInvWeight(1).setStackMode(MossItem.StackMode.STACK_UNIT).setDisplayName("An unidentifiable item").setInternalName("sys:unknown").createMossItem();


    public MossItem getItem(short itemId) {
        return this.defItems.get(itemId);
    }




    public void putNodeAlias(String alias, String dst) {
        MossItem dstItem = this.defItemsByName.get(dst);
        this.defItemsByName.put(alias, dstItem);
    }


    public MossItem getItem(String string) {

        MossItem r = this.defItemsByName.get(string);
        return r == null ? this.unknownFallbackItem : r;
    }

    public ItemManager() {

    }

    private HashMap<MapNode, MossItem> itemsForNode = new HashMap<>();


    public MossItem getForNode(MapNode nd) {
        MossItem item = itemsForNode.get(nd);
        if (item == null) {
            // item = createForNode(MossItem); //FIXME
            itemsForNode.put(nd, item);
            return item;
        } else
            return item;
    }

    {

        defItems.add(this.unknownFallbackItem);
        itemsForNode.put(LocalNodeManager.getUnknownFallbackNode(),
                this.unknownFallbackItem);
    }

	/*
     * private static MossItem createForNode(MossItem MossItem) { MossItem
	 * mi=new MossItem(MossItem.texture, MossItem, 1, true,
	 * MossItem.userFacingName, MossItem.nodeName) return mi; //TODO todo }
	 */
    // FIXME above
}
