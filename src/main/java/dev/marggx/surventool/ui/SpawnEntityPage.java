package dev.marggx.surventool.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.*;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventItemMerging;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.pages.EntitySpawnPage;
import dev.marggx.surventool.utils.Inventory;
import dev.marggx.surventool.utils.Logger;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.joml.Vector3d;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpawnEntityPage extends InteractiveCustomUIPage<SpawnEntityPage.PageData> {
    private static final Logger LOGGER = Logger.get();
    @Nonnull
    private String searchQuery = "";
    @Nullable
    private String selectedItemId;
    @Nullable
    private Ref<EntityStore> modelPreview;
    private Vector3d position;
    private Rotation3f rotation;
    private float currentRotationOffset = 0.0F;
    private float currentScale = 1.0F;
    private float lastPreviewScale = 1.0F;
    private long lastScaleUpdateTime = 0L;
    Inventory.InventoryScan invScan;

    public SpawnEntityPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        invScan = Inventory.collectNearbyItems(playerRef.getReference().getStore().getExternalData().getWorld(), playerRef.getReference(), playerRef.getReference().getStore(), 15, true);
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder cBuilder, @NonNullDecl UIEventBuilder eBuilder, @NonNullDecl Store<EntityStore> store) {
        cBuilder.append("Pages/SpawnEntityPage.ui");
        eBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput", EventData.of("@SearchQuery", "#SearchInput.Value"), false);
        eBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#RotationOffset",
                new EventData().append(PageData.ACTION, PageData.Action.UpdateRotationOffset).append(PageData.ROTATION_OFFSET, "#RotationOffset.Value"),
                false
        );
        eBuilder.addEventBinding(
                CustomUIEventBindingType.MouseButtonReleased,
                "#RotationOffset",
                new EventData().append(PageData.ACTION, PageData.Action.RotationReleased).append(PageData.ROTATION_OFFSET, "#RotationOffset.Value"),
                false
        );
        eBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged, "#ScaleSlider", 
                new EventData().append(PageData.ACTION, PageData.Action.UpdateScale).append(PageData.SCALE, "#ScaleSlider.Value"),
                false
        );
        eBuilder.addEventBinding(
                CustomUIEventBindingType.MouseButtonReleased,
                "#ScaleSlider",
                new EventData().append(PageData.ACTION, PageData.Action.ScaleReleased).append(PageData.SCALE, "#ScaleSlider.Value"),
                false
        );
        eBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#Spawn",
                new EventData().append(PageData.ACTION, PageData.Action.Spawn).append(PageData.SCALE, "#ScaleSlider.Value"),
                false
        );
        eBuilder.addEventBinding(
                CustomUIEventBindingType.Dropped,
                "#ItemMaterialSlot",
                new EventData().append(PageData.ACTION, PageData.Action.SetItemMaterial),
                false
        );
        eBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ClearMaterial",
                new EventData().append(PageData.ACTION, PageData.Action.ClearMaterial),
                false
        );
        this.buildItemsContent(ref, store, cBuilder, eBuilder);
    }

    private void buildItemsContent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cBuilder, @Nonnull UIEventBuilder eBuilder) {
        if (this.selectedItemId != null) {

            Item item = (Item)Item.getAssetMap().getAsset(this.selectedItemId);
            if (item != null) {
                cBuilder.set("#SelectedName.Text", this.selectedItemId);
                cBuilder.set("#ItemMaterialSlot.Slots", new ItemGridSlot[]{new ItemGridSlot(new ItemStack(this.selectedItemId, 1))});
                cBuilder.set("#ClearMaterial.Visible", true);
                cBuilder.set("#DropIndicator.Visible", false);
            }
        } else {
            cBuilder.set("#SelectedName.Text", Message.translation("server.customUI.entitySpawnPage.selectAnItem"));
            cBuilder.set("#ItemMaterialSlot.Slots", new ItemGridSlot[]{new ItemGridSlot()});
            cBuilder.set("#ClearMaterial.Visible", false);
            cBuilder.set("#DropIndicator.Visible", true);
        }

        int rowIndex = 0;
        int columnIndex = 0;
        List<ItemGridSlot> slotList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : invScan.items().entrySet()) {
            if (columnIndex == 0) {
                cBuilder.appendInline("#AvailableItemsList", "Group { LayoutMode: Left; Anchor: (Bottom: 0); }");
            }
            cBuilder.append("#AvailableItemsList[" + rowIndex + "]", "Pages/ItemSlot.ui");
            cBuilder.set("#AvailableItemsList[" + rowIndex + "][" + columnIndex + "] #ItemSlot.ItemId", entry.getKey());
            cBuilder.set("#AvailableItemsList[" + rowIndex + "][" + columnIndex + "] #ItemSlot.Quantity", 1);
            eBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#AvailableItemsList[" + rowIndex + "][" + columnIndex + "]",
                    new EventData().append(PageData.ACTION, PageData.Action.AvailableItemSelected).append(PageData.ITEM_ID, entry.getKey()),
                    false
            );

            columnIndex++;
            if (columnIndex >= 6) {
                columnIndex = 0;
                rowIndex++;
            }
        }
    }

    private void selectItem(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull String itemId, @Nonnull UICommandBuilder commandBuilder) {
        Item item = (Item)Item.getAssetMap().getAsset(itemId);
        if (item != null) {
            this.selectedItemId = itemId;
            commandBuilder.set("#SelectedName.Text", itemId);
            commandBuilder.set("#ItemMaterialSlot.Slots", new ItemGridSlot[]{new ItemGridSlot(new ItemStack(itemId, 1))});
            commandBuilder.set("#ClearMaterial.Visible", true);
            commandBuilder.set("#DropIndicator.Visible", false);
            Model model = this.getItemModel(item);
            if (model != null) {
                this.createOrUpdatePreview(ref, store, commandBuilder, model);
            } else if (item.hasBlockType()) {
                this.createOrUpdateBlockPreview(ref, store, itemId);
            } else {
                this.createOrUpdateItemPreview(ref, store, itemId);
            }
        }
    }

    private void createOrUpdatePreview(
            @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder commandBuilder, @Nullable Model model
    ) {
        if (model != null) {
            if (this.modelPreview != null && this.modelPreview.isValid()) {
                store.putComponent(this.modelPreview, ModelComponent.getComponentType(), new ModelComponent(model));
            } else {
                this.initPosition(ref, store);
                Holder<EntityStore> holder = store.getRegistry().newHolder();
                holder.addComponent(NetworkId.getComponentType(), new NetworkId(((EntityStore)store.getExternalData()).takeNextNetworkId()));
                holder.addComponent(EntityStore.REGISTRY.getNonSerializedComponentType(), NonSerialized.get());
                Rotation3f previewRotation = new Rotation3f(this.rotation);
                previewRotation.setYaw(this.rotation.yaw() + this.currentRotationOffset);
                holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(this.position, previewRotation));
                holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
                holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(previewRotation));
                this.modelPreview = store.addEntity(holder, AddReason.SPAWN);
                this.lastPreviewScale = this.currentScale;
            }
        }
    }

    private void clearPreview(@Nonnull Store<EntityStore> store) {
        if (this.modelPreview != null && this.modelPreview.isValid()) {
            store.removeEntity(this.modelPreview, RemoveReason.REMOVE);
        }

        this.modelPreview = null;
    }

    private void spawnItem(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, int count) {
        if (this.selectedItemId != null && this.position != null && this.rotation != null) {
            if (count >= 1 && count <= 100) {
                Item item = Item.getAssetMap().getAsset(this.selectedItemId);
                if (item != null) {
                    this.clearPreview(store);
                    Rotation3f spawnRotation = new Rotation3f(this.rotation);
                    spawnRotation.setYaw(this.rotation.yaw() + this.currentRotationOffset);
                    Model model = this.getItemModel(item);
                    if (model != null) {
                        String modelId = this.getItemModelId(item);

                        for (int i = 0; i < count; i++) {
                            Holder<EntityStore> holder = store.getRegistry().newHolder();
                            holder.addComponent(NetworkId.getComponentType(), new NetworkId(((EntityStore)store.getExternalData()).takeNextNetworkId()));
                            holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(this.position, spawnRotation));
                            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
                            holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(new Model.ModelReference(modelId, this.currentScale, null, true)));
                            ItemStack itemStack = new ItemStack(this.selectedItemId, 1);
                            itemStack.setOverrideDroppedItemAnimation(true);
                            holder.addComponent(ItemComponent.getComponentType(), new ItemComponent(itemStack));
                            holder.addComponent(PreventItemMerging.getComponentType(), PreventItemMerging.INSTANCE);
                            holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(spawnRotation));
                            holder.addComponent(PropComponent.getComponentType(), PropComponent.get());
                            holder.ensureComponent(UUIDComponent.getComponentType());
                            holder.addComponent(Interactable.getComponentType(), Interactable.INSTANCE);
                            Interactions interactions = new Interactions();
                            holder.addComponent(Interactions.getComponentType(), interactions);
                            interactions.setInteractionId(InteractionType.Use, "*PickupItem");
                            interactions.setInteractionHint("server.interactionHints.pickup");
                            store.addEntity(holder, AddReason.SPAWN);
                        }
                    } else if (item.hasBlockType()) {
                        for (int i = 0; i < count; i++) {
                            Holder<EntityStore> holder = store.getRegistry().newHolder();
                            holder.addComponent(BlockEntity.getComponentType(), new BlockEntity(this.selectedItemId));
                            holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(this.position, spawnRotation));
                            holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(this.currentScale * 2.0F));
                            ItemStack itemStack = new ItemStack(this.selectedItemId, 1);
                            itemStack.setOverrideDroppedItemAnimation(true);
                            holder.addComponent(ItemComponent.getComponentType(), new ItemComponent(itemStack));
                            holder.addComponent(PreventItemMerging.getComponentType(), PreventItemMerging.INSTANCE);
                            holder.addComponent(PropComponent.getComponentType(), PropComponent.get());
                            holder.ensureComponent(UUIDComponent.getComponentType());
                            holder.addComponent(Interactable.getComponentType(), Interactable.INSTANCE);
                            Interactions interactions = new Interactions();
                            holder.addComponent(Interactions.getComponentType(), interactions);
                            interactions.setInteractionId(InteractionType.Use, "*PickupItem");
                            interactions.setInteractionHint("server.interactionHints.pickup");
                            store.addEntity(holder, AddReason.SPAWN);
                        }
                    } else {
                        for (int i = 0; i < count; i++) {
                            Holder<EntityStore> holder = store.getRegistry().newHolder();
                            holder.addComponent(NetworkId.getComponentType(), new NetworkId(((EntityStore)store.getExternalData()).takeNextNetworkId()));
                            holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(this.position, spawnRotation));
                            ItemStack itemStack = new ItemStack(this.selectedItemId, 1);
                            itemStack.setOverrideDroppedItemAnimation(true);
                            holder.addComponent(ItemComponent.getComponentType(), new ItemComponent(itemStack));
                            holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(this.currentScale));
                            holder.addComponent(PreventItemMerging.getComponentType(), PreventItemMerging.INSTANCE);
                            holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(spawnRotation));
                            holder.addComponent(PropComponent.getComponentType(), PropComponent.get());
                            holder.addComponent(Interactable.getComponentType(), Interactable.INSTANCE);
                            Interactions interactions = new Interactions();
                            holder.addComponent(Interactions.getComponentType(), interactions);
                            interactions.setInteractionId(InteractionType.Use, "*PickupItem");
                            interactions.setInteractionHint("server.interactionHints.pickup");
                            store.addEntity(holder, AddReason.SPAWN);
                        }
                    }

                    Inventory.removeItemFromAnyInventory(invScan.scannedInventories(), this.selectedItemId, ref, store);
                    ((Player)store.getComponent(ref, Player.getComponentType())).getPageManager().setPage(ref, store, Page.None);
                    this.playerRef
                            .sendMessage(Message.translation("server.customUI.entitySpawnPage.spawnedItem").param("quantity", count).param("item", this.selectedItemId));
                }
            }
        }
    }

    @Nullable
    private String getItemModelId(@Nonnull Item item) {
        String modelId = item.getModel();
        if (modelId == null && item.hasBlockType()) {
            BlockType blockType = (BlockType)BlockType.getAssetMap().getAsset(item.getId());
            if (blockType != null && blockType.getCustomModel() != null) {
                modelId = blockType.getCustomModel();
            }
        }

        return modelId;
    }

    @Nullable
    private Model getItemModel(@Nonnull Item item) {
        String modelId = this.getItemModelId(item);
        if (modelId == null) {
            return null;
        } else {
            ModelAsset modelAsset = (ModelAsset)ModelAsset.getAssetMap().getAsset(modelId);
            return modelAsset != null ? Model.createStaticScaledModel(modelAsset, this.currentScale) : null;
        }
    }


    private void updatePreviewScale(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (this.modelPreview != null && this.modelPreview.isValid()) {
            EntityScaleComponent existingScale = (EntityScaleComponent)store.getComponent(this.modelPreview, EntityScaleComponent.getComponentType());
            if (existingScale != null) {
                boolean hasBlock = store.getComponent(this.modelPreview, BlockEntity.getComponentType()) != null;
                existingScale.setScale(hasBlock ? this.currentScale * 2.0F : this.currentScale);
            }
        }
    }

    private void initPosition(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        TransformComponent transformComponent = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());

        assert transformComponent != null;

        HeadRotation headRotationComponent = (HeadRotation)store.getComponent(ref, HeadRotation.getComponentType());

        assert headRotationComponent != null;

        Vector3d playerPosition = transformComponent.getPosition();
        Rotation3f headRotation = headRotationComponent.getRotation();
        Vector3d direction = Transform.getDirection(headRotation.pitch(), headRotation.yaw());
        Vector3d lookTarget = TargetUtil.getTargetLocation(ref, 4.0, store);
        Vector3d previewPosition;
        if (lookTarget != null) {
            previewPosition = lookTarget;
        } else {
            Vector3d aheadPosition = new Vector3d(playerPosition).add(new Vector3d(direction).mul(4.0));
            World world = ((EntityStore)store.getExternalData()).getWorld();
            Vector3i groundTarget = TargetUtil.getTargetBlock(
                    world, (blockId, fluidId) -> blockId != 0, aheadPosition.x, aheadPosition.y + 0.5, aheadPosition.z, 0.0, -1.0, 0.0, 3.0
            );
            if (groundTarget != null) {
                previewPosition = new Vector3d(groundTarget.x + 0.5, groundTarget.y + 1, groundTarget.z + 0.5);
            } else {
                previewPosition = aheadPosition;
            }
        }

        Vector3d relativePos = new Vector3d(playerPosition).sub(previewPosition);
        relativePos.y = 0.0;
        Rotation3f previewRotation = Rotation3f.lookAt(relativePos);
        this.position = previewPosition;
        this.rotation = previewRotation;
    }

    private void createOrUpdateBlockPreview(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull String blockTypeKey) {
        this.clearPreview(store);
        this.initPosition(ref, store);
        Holder<EntityStore> holder = store.getRegistry().newHolder();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(((EntityStore)store.getExternalData()).takeNextNetworkId()));
        holder.addComponent(EntityStore.REGISTRY.getNonSerializedComponentType(), NonSerialized.get());
        Rotation3f previewRotation = new Rotation3f(this.rotation);
        previewRotation.setYaw(this.rotation.yaw() + this.currentRotationOffset);
        holder.addComponent(BlockEntity.getComponentType(), new BlockEntity(blockTypeKey));
        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(this.position, previewRotation));
        holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(this.currentScale * 2.0F));
        this.modelPreview = store.addEntity(holder, AddReason.SPAWN);
        this.lastPreviewScale = this.currentScale;
    }

    private void createOrUpdateItemPreview(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull String itemId) {
        this.clearPreview(store);
        this.initPosition(ref, store);
        Holder<EntityStore> holder = store.getRegistry().newHolder();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(((EntityStore)store.getExternalData()).takeNextNetworkId()));
        holder.addComponent(EntityStore.REGISTRY.getNonSerializedComponentType(), NonSerialized.get());
        ItemStack itemStack = new ItemStack(itemId, 1);
        itemStack.setOverrideDroppedItemAnimation(true);
        holder.addComponent(ItemComponent.getComponentType(), new ItemComponent(itemStack));
        holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(this.currentScale));
        Rotation3f previewRotation = new Rotation3f(this.rotation);
        previewRotation.setYaw(this.rotation.yaw() + this.currentRotationOffset);
        holder.addComponent(PreventPickup.getComponentType(), PreventPickup.INSTANCE);
        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(this.position, previewRotation));
        holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(previewRotation));
        this.modelPreview = store.addEntity(holder, AddReason.SPAWN);
        this.lastPreviewScale = this.currentScale;
    }

    private void clearSelectedItem(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        this.selectedItemId = null;
        this.clearPreview(store);
        UICommandBuilder commandBuilder = new UICommandBuilder();
        commandBuilder.set("#SelectedName.Text", Message.translation("server.customUI.entitySpawnPage.selectAnItem"));
        commandBuilder.set("#ItemMaterialSlot.Slots", new ItemGridSlot[]{new ItemGridSlot()});
        commandBuilder.set("#ClearMaterial.Visible", false);
        commandBuilder.set("#DropIndicator.Visible", true);
        this.sendUpdate(commandBuilder, (UIEventBuilder)null, false);
    }

    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull SpawnEntityPage.PageData data) {
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        assert playerComponent != null;
        PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
        assert playerRefComponent != null;

        UICommandBuilder cBuilder = new UICommandBuilder();

        switch (data.action) {
            case Spawn:
                this.spawnItem(ref, store,1);
                this.sendUpdate(cBuilder, (UIEventBuilder)null, false);
                break;
            case AvailableItemSelected:
                this.selectItem(ref, store, data.itemId, cBuilder);
                this.sendUpdate(cBuilder, (UIEventBuilder)null, false);
                break;
            case ClearMaterial:
                this.clearSelectedItem(ref, store);
                this.sendUpdate(cBuilder, (UIEventBuilder)null, false);
                break;
            case UpdateRotationOffset:
                float rawRotation = data.rotationOffset;
                data.rotationOffset = (float)Math.clamp((long)Math.round(data.rotationOffset), -180, 180);
                this.currentRotationOffset = (float)Math.toRadians((double)data.rotationOffset);
                if (rawRotation != data.rotationOffset) {
                    cBuilder.set("#RotationOffset.Value", (int)data.rotationOffset);
                }

                cBuilder.set("#RotationValue.Text", String.format("%d°", (int)data.rotationOffset));
                this.sendUpdate(cBuilder, (UIEventBuilder)null, false);
                if (this.modelPreview != null && this.modelPreview.isValid()) {
                    TransformComponent transform = (TransformComponent)store.getComponent(this.modelPreview, TransformComponent.getComponentType());
                    transform.getRotation().setYaw(this.rotation.yaw() + this.currentRotationOffset);
                    HeadRotation headRotation = (HeadRotation)store.getComponent(this.modelPreview, HeadRotation.getComponentType());
                    if (headRotation != null) {
                        headRotation.getRotation().setYaw(this.rotation.yaw() + this.currentRotationOffset);
                    }
                }
                break;
            case RotationReleased:
                data.rotationOffset = (float)Math.clamp((long)Math.round(data.rotationOffset), -180, 180);
                this.currentRotationOffset = (float)Math.toRadians((double)data.rotationOffset);
                cBuilder.set("#RotationValue.Text", String.format("%d°", (int)data.rotationOffset));
                this.sendUpdate(cBuilder, (UIEventBuilder)null, false);
                if (this.modelPreview != null && this.modelPreview.isValid()) {
                    TransformComponent transform = (TransformComponent)store.getComponent(this.modelPreview, TransformComponent.getComponentType());
                    transform.getRotation().setYaw(this.rotation.yaw() + this.currentRotationOffset);
                    HeadRotation headRotation = (HeadRotation)store.getComponent(this.modelPreview, HeadRotation.getComponentType());
                    if (headRotation != null) {
                        headRotation.getRotation().setYaw(this.rotation.yaw() + this.currentRotationOffset);
                    }
                }
                break;
            case UpdateScale:
                if (data.scale < 0.1F) {
                    break;
                }
                float rawScale = data.scale;
                this.currentScale = Math.clamp((float)Math.round(data.scale * 10.0F) / 10.0F, 0.1F, 5.0F);
                if (rawScale != this.currentScale) {
                    cBuilder.set("#ScaleSlider.Value", this.currentScale);
                }

                cBuilder.set("#ScaleValue.Text", String.format("%.1f", this.currentScale));
                this.sendUpdate(cBuilder, (UIEventBuilder)null, false);
                long now = System.currentTimeMillis();
                if (this.modelPreview != null && this.modelPreview.isValid() && this.currentScale != this.lastPreviewScale && now - this.lastScaleUpdateTime >= 200L) {
                    this.lastScaleUpdateTime = now;
                    this.lastPreviewScale = this.currentScale;
                    this.updatePreviewScale(ref, store);
                }
                break;
            case ScaleReleased:
                if (this.selectedItemId == null) {
                    break;
                }
                if (data.scale < 0.1F) {
                    break;
                }
                this.currentScale = Math.clamp((float)Math.round(data.scale * 10.0F) / 10.0F, 0.1F, 5.0F);
                UICommandBuilder scaleReleasedCmd = new UICommandBuilder();
                scaleReleasedCmd.set("#ScaleValue.Text", String.format("%.1f", this.currentScale));
                this.sendUpdate(scaleReleasedCmd, null, false);
                Item item = Item.getAssetMap().getAsset(this.selectedItemId);
                if (item != null) {
                    Model model = this.getItemModel(item);
                    if (model != null) {
                        this.createOrUpdatePreview(ref, store, cBuilder, model);
                    } else if (item.hasBlockType()) {
                        this.createOrUpdateBlockPreview(ref, store, this.selectedItemId);
                    } else {
                        this.createOrUpdateItemPreview(ref, store, this.selectedItemId);
                    }
                }
                break;
        }
    }

    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        this.clearPreview(store);
    }


    public static class PageData {
        public enum Action {
            Save,
            Cancel,
            AvailableItemSelected,
            UpdateRotationOffset,
            RotationReleased,
            UpdateScale,
            ScaleReleased,
            Spawn,
            SetItemMaterial,
            ClearMaterial;
            
            Action() {
            }
        }

        public static final String ACTION = "Action";
        public static final String ROTATION_OFFSET = "@RotationOffset";
        public static final String SCALE = "@Scale";
        public static final String ITEM_ID = "ItemId";
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(
                        new KeyedCodec<>(ACTION, new EnumCodec<>(PageData.Action.class, EnumCodec.EnumStyle.LEGACY)),
                        (o, action) -> o.action = action,
                        o -> o.action
                )
                .add()
                .append(
                        new KeyedCodec<>(ITEM_ID, Codec.STRING),
                        (entry, s) -> entry.itemId = s,
                        (entry) -> entry.itemId
                )
                .add()
                .append(
                        new KeyedCodec<>(ROTATION_OFFSET, Codec.FLOAT),
                        (entry, s) -> entry.rotationOffset = s,
                        (entry) -> entry.rotationOffset
                )
                .add()
                .append(
                        new KeyedCodec<>(SCALE, Codec.FLOAT),
                        (entry, s) -> entry.scale = s,
                        (entry) -> entry.scale
                )
                .add()
                .build();
        public Action action;
        public String value;
        public String itemId;
        private float rotationOffset;
        private float scale;

        public PageData() {
        }
    }
}