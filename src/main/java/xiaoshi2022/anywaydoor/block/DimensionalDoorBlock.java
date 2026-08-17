package xiaoshi2022.anywaydoor.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import xiaoshi2022.anywaydoor.block.entity.DimensionalDoorBlockEntity;
import xiaoshi2022.anywaydoor.teleport.DimensionalDoorTeleporter;

public class DimensionalDoorBlock extends Block implements EntityBlock {

	private static final VoxelShape SHAPE = makeShape();

	public DimensionalDoorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DimensionalDoorBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}

		if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			if (level.getBlockEntity(pos) instanceof DimensionalDoorBlockEntity door) {
				if (door.isOpen()) {
					DimensionalDoorTeleporter.closeDoor(level, pos, serverPlayer);
				} else {
					DimensionalDoorTeleporter.openDoor(level, pos, serverPlayer);
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
		return SHAPE;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
		return SHAPE;
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return SHAPE;
	}

	@Override
	public boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	// ========== 挖掉方块时清理所有传送门 ==========
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!level.isClientSide && !state.is(newState.getBlock())) {
			if (level.getBlockEntity(pos) instanceof DimensionalDoorBlockEntity door) {
				// 清理所有传送门（包括反向的）
				door.cleanupAllPortals();
			}
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	private static VoxelShape makeShape() {
		VoxelShape shape = Shapes.empty();

		shape = Shapes.join(shape, Shapes.box(0, 0.125, 0, 1, 0.1875, 0.125), BooleanOp.OR);
		shape = Shapes.join(shape, Shapes.box(0, 2.1875, 0, 1, 2.25, 0.125), BooleanOp.OR);
		shape = Shapes.join(shape, Shapes.box(1, 0.125, 0, 1.0625, 2.25, 0.125), BooleanOp.OR);
		shape = Shapes.join(shape, Shapes.box(-0.0625, 0.125, 0, 0, 2.25, 0.125), BooleanOp.OR);
		shape = Shapes.join(shape, Shapes.box(-0.075, 2.21875, -0.03125, 1.08125, 2.2875, 0.15625), BooleanOp.OR);
		shape = Shapes.join(shape, Shapes.box(-0.1, 2.2875, -0.03125, 1.1125, 2.35625, 0.15625), BooleanOp.OR);
		shape = Shapes.join(shape, Shapes.box(-0.075, 0.05625, -0.03125, 1.08125, 0.11875, 0.15625), BooleanOp.OR);
		shape = Shapes.join(shape, Shapes.box(0.98125, -0.00625, -0.03125, 1.1125, 0.0625, 0.15625), BooleanOp.OR);
		shape = Shapes.join(shape, Shapes.box(-0.1, -0.00625, -0.03125, 0.03125, 0.0625, 0.15625), BooleanOp.OR);

		return shape;
	}
}