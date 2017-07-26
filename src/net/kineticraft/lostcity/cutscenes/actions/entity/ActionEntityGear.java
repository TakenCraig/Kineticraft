package net.kineticraft.lostcity.cutscenes.actions.entity;

import lombok.Getter;
import net.kineticraft.lostcity.cutscenes.CutsceneEvent;
import net.kineticraft.lostcity.data.maps.JsonMap;
import net.kineticraft.lostcity.utils.Utils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Set the gear of an entity.
 * Created by Kneesnap on 7/22/2017.
 */
@Getter
public class ActionEntityGear extends ActionEntity {
    private JsonMap<ItemStack> items = new JsonMap<>();

    @Override
    public void execute(CutsceneEvent event) {
        LivingEntity e = getLivingEntity(event);
        getItems().toEnumMap(EquipmentSlot.class).forEach((k, v) -> Utils.setItem(e, k, v));
    }
}