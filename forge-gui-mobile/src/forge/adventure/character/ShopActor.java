package forge.adventure.character;

import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.adventure.data.ShopData;
import forge.adventure.scene.RewardScene;
import forge.adventure.stage.MapStage;
import forge.adventure.util.Reward;

/**
 * Map actor that will open the Shop on collision
 */
public class ShopActor extends MapActor{
    private final MapStage stage;
    private ShopData shopData;
    Array<Reward> rewardData;

    public ShopActor(MapStage stage, int id, Array<Reward> rewardData, ShopData data)
    {
        super(id);
        this.stage = stage;
        this.shopData = data;
        this.rewardData = rewardData;

    }

    public MapStage getMapStage()
    {
        return stage;
    }

    @Override
    public void  onPlayerCollide()
    {

        stage.getPlayerSprite().stop();
         RewardScene.instance().loadRewards(rewardData, RewardScene.Type.Shop,this);
        Forge.switchScene(RewardScene.instance());
    }


    public boolean isUnlimited() {
        return shopData.unlimited;
    }

    @Override
    public String getName() {
        return shopData.name;
    }

    public String getDescription() {
        return shopData.description;
    }
}
