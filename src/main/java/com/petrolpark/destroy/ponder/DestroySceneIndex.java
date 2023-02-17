package com.petrolpark.destroy.ponder;

import com.petrolpark.destroy.Destroy;
import com.petrolpark.destroy.block.DestroyBlocks;
import com.petrolpark.destroy.item.DestroyItems;
import com.simibubi.create.foundation.ponder.PonderRegistrationHelper;
import com.simibubi.create.foundation.ponder.PonderRegistry;
import com.simibubi.create.foundation.ponder.PonderTag;

public class DestroySceneIndex {

    public static final PonderTag DESTROY = new PonderTag(Destroy.asResource("destroy"))
        .item(DestroyBlocks.CENTRIFUGE.get())
        .addToIndex();

    private static final PonderRegistrationHelper HELPER = new PonderRegistrationHelper(Destroy.MOD_ID);

    public static void register() {
        HELPER.addStoryBoard(DestroyBlocks.CENTRIFUGE, "centrifuge", DestroyScenes::centrifuge, DESTROY, PonderTag.FLUIDS);
        HELPER.addStoryBoard(DestroyItems.HYPERACCUMULATING_FERTILIZER, "phytomining", DestroyScenes::phytomining, DESTROY);

        PonderRegistry.TAGS.forTag(DESTROY)
            .add(DestroyBlocks.CENTRIFUGE)
            .add(DestroyItems.HYPERACCUMULATING_FERTILIZER)
        ;

        PonderRegistry.TAGS.forTag(PonderTag.FLUIDS)
            .add(DestroyBlocks.CENTRIFUGE)
        ;
    };
};
