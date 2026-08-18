package xiaoshi2022.anywaydoor.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import xiaoshi2022.anywaydoor.block.entity.DimensionalDoorBlockEntity;
import xiaoshi2022.anywaydoor.teleport.DimensionalDoorTeleporter;

public class DimensionalDoorBlock extends Block implements EntityBlock {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	// 不同朝向的碰撞箱
	private static final VoxelShape SHAPE_NORTH = makeShape(Direction.NORTH);
	private static final VoxelShape SHAPE_SOUTH = makeShape(Direction.SOUTH);
	private static final VoxelShape SHAPE_EAST = makeShape(Direction.EAST);
	private static final VoxelShape SHAPE_WEST = makeShape(Direction.WEST);

	public DimensionalDoorBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DimensionalDoorBlockEntity(pos, state);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		return this.defaultBlockState().setValue(FACING, facing);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
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

	// ========== 根据朝向返回对应的碰撞箱 ==========
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
		Direction facing = state.getValue(FACING);
		return switch (facing) {
			case NORTH -> SHAPE_NORTH;
			case SOUTH -> SHAPE_SOUTH;
			case EAST -> SHAPE_EAST;
			case WEST -> SHAPE_WEST;
			default -> SHAPE_NORTH;
		};
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
		return getShape(state, level, pos, context);
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return getShape(state, level, pos, net.minecraft.world.phys.shapes.CollisionContext.empty());
	}

	@Override
	public boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!level.isClientSide && !state.is(newState.getBlock())) {
			if (level.getBlockEntity(pos) instanceof DimensionalDoorBlockEntity door) {
				// ========== 如果是反向门，清理父门关联 ==========
				if (door.isReverseDoor()) {
					// 清理父门关联已经在 setRemoved 中处理了
					// 但这里额外调用一次确保清理
					door.cleanupAllPortals();
				} else {
					// 普通门：清理所有关联
					door.cleanupAllPortals();
				}
			}
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	// ========== 生成朝向对应的碰撞箱 ==========
	private static VoxelShape makeShape(Direction facing) {
		VoxelShape shape = Shapes.empty();

		// 默认朝北 (Z负方向)
		boolean isNorth = facing == Direction.NORTH;
		boolean isSouth = facing == Direction.SOUTH;
		boolean isEast = facing == Direction.EAST;
		boolean isWest = facing == Direction.WEST;

		// 底部门槛
		shape = Shapes.join(shape, box(0, 0.125, 0, 1, 0.1875, 0.125, facing), BooleanOp.OR);

		// 顶部横梁
		shape = Shapes.join(shape, box(0, 2.1875, 0, 1, 2.25, 0.125, facing), BooleanOp.OR);

		// 右侧门框
		shape = Shapes.join(shape, box(1, 0.125, 0, 1.0625, 2.25, 0.125, facing), BooleanOp.OR);

		// 左侧门框
		shape = Shapes.join(shape, box(-0.0625, 0.125, 0, 0, 2.25, 0.125, facing), BooleanOp.OR);

		// 门框装饰（外框）- 顶部
		shape = Shapes.join(shape, box(-0.075, 2.21875, -0.03125, 1.08125, 2.2875, 0.15625, facing), BooleanOp.OR);
		shape = Shapes.join(shape, box(-0.1, 2.2875, -0.03125, 1.1125, 2.35625, 0.15625, facing), BooleanOp.OR);

		// 门框装饰（底部）
		shape = Shapes.join(shape, box(-0.075, 0.05625, -0.03125, 1.08125, 0.11875, 0.15625, facing), BooleanOp.OR);
		shape = Shapes.join(shape, box(0.98125, -0.00625, -0.03125, 1.1125, 0.0625, 0.15625, facing), BooleanOp.OR);
		shape = Shapes.join(shape, box(-0.1, -0.00625, -0.03125, 0.03125, 0.0625, 0.15625, facing), BooleanOp.OR);

		return shape;
	}

	// ========== 根据朝向旋转坐标 ==========
	private static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2, Direction facing) {
		return switch (facing) {
			case NORTH -> Shapes.box(x1, y1, z1, x2, y2, z2);
			case SOUTH -> Shapes.box(1 - x2, y1, 1 - z2, 1 - x1, y2, 1 - z1);
			case EAST -> Shapes.box(1 - z2, y1, x1, 1 - z1, y2, x2);
			case WEST -> Shapes.box(z1, y1, 1 - x2, z2, y2, 1 - x1);
			default -> Shapes.box(x1, y1, z1, x2, y2, z2);
		};
	}
}