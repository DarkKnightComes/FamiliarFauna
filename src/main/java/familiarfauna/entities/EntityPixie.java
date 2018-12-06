/*******************************************************************************
 * Copyright 2015-2016, the Biomes O' Plenty Team
 * 
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 * 
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/

package familiarfauna.entities;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import familiarfauna.api.FFItems;
import familiarfauna.api.FFSounds;
import familiarfauna.config.ConfigurationHandler;
import familiarfauna.core.FamiliarFauna;
import familiarfauna.init.ModLootTable;
import familiarfauna.item.ItemBugHabitat;
import familiarfauna.particle.FFParticleTypes;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityFlying;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

public class EntityPixie extends EntityAmbientCreature implements EntityFlying
{
	private static final DataParameter<Byte> TYPE = EntityDataManager.<Byte>createKey(EntityPixie.class, DataSerializers.BYTE);
	
    public EntityPixie(World worldIn) {
        super(worldIn);
        this.setSize(0.7F, 0.7F);
        
        this.moveHelper = new EntityPixie.PixieMoveHelper();
        this.tasks.addTask(3, new EntityPixie.AIPixieRandomFly());
    }
    
    @Override
    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(2.0D);
    }
    
    @Override
	protected void entityInit()
    {
        super.entityInit();
        this.dataManager.register(TYPE, Byte.valueOf((byte)0));
    }
    
    @Override
    protected SoundEvent getAmbientSound()
    {
        return FFSounds.pixie_ambient;
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource source)
    {
        return FFSounds.pixie_hurt;
    }
    
    @Nullable
    @Override
    protected ResourceLocation getLootTable()
    {
        return ModLootTable.PIXIE_LOOT;
    }
    
    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand)
    {
        ItemStack itemstack = player.getHeldItem(hand);

        if (itemstack.getItem() == FFItems.bug_net && !player.capabilities.isCreativeMode && !this.isChild())
        {
            ItemStack emptyHabitat = findEmptyHabitatStack(player);
            if (emptyHabitat != ItemStack.EMPTY)
            {
                //player.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
                player.swingArm(hand);
                emptyHabitat.shrink(1);
                itemstack.damageItem(1, player);
                this.setDead();
                
                ItemStack habitat = new ItemStack(FFItems.bug_habitat);
                habitat.setTagCompound(new NBTTagCompound());
                habitat.getTagCompound().setString("Bug", "pixie");
                
                if (this.getCustomNameTag() != "")
                {
                    habitat.getTagCompound().setString("Name", this.getCustomNameTag());
                }
                
                habitat.getTagCompound().setInteger("Type", this.getPixieType());

                if (!player.inventory.addItemStackToInventory(habitat))
                {
                    player.dropItem(habitat, false);
                }
    
                return true;
            }
            else
            {
                return super.processInteract(player, hand);
            }
        }
        else
        {
            return super.processInteract(player, hand);
        }
    }
    
    @Nonnull
    public static ItemStack findEmptyHabitatStack(EntityPlayer player)
    {
        //Search every item in the player's main inventory for a bug habitat
        for (ItemStack stack : player.inventory.mainInventory)
        {
            if (isHabitatEmpty(stack))
            {
                return stack;
            }
        }
        
        return ItemStack.EMPTY;
    }
    
    public static boolean isHabitatEmpty(@Nonnull ItemStack stack)
    {
        if (!stack.isEmpty() && stack.getItem() instanceof ItemBugHabitat)
        {
            if (stack.hasTagCompound() && stack.getTagCompound().hasKey("Bug"))
            {
                return false;
            }
            else
            {
                return true;
            }
        }
        
        return false;
    }
    
    public void writeEntityToNBT(NBTTagCompound tagCompound)
    {
        super.writeEntityToNBT(tagCompound);
        tagCompound.setInteger("PixieType", this.getPixieType());
    }

    public void readEntityFromNBT(NBTTagCompound tagCompund)
    {
        super.readEntityFromNBT(tagCompund);
        this.setPixieType(tagCompund.getInteger("PixieType"));
    }
    
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData livingdata)
    {
        livingdata = super.onInitialSpawn(difficulty, livingdata);
        int i = this.world.rand.nextInt(3);

        livingdata = new EntityPixie.PixieTypeData(i);
        this.setPixieType(i);

        return livingdata;
    }
    
    public int getPixieType()
    {
        return (int) this.dataManager.get(TYPE);
    }
    
    public void setPixieType(int pixieTypeId)
    {
        this.dataManager.set(TYPE, Byte.valueOf((byte)pixieTypeId));
    }
    
    public static class PixieTypeData implements IEntityLivingData
    {
        public int typeData;

        public PixieTypeData(int type)
        {
            this.typeData = type;
        }
    }
    
    @Override
    public void onLivingUpdate()
    {
        super.onLivingUpdate();
        
        if (this.world.isRemote)
        {
            if (world.rand.nextInt(4) == 0)
            {
            	FFParticleTypes particle = FFParticleTypes.PINK_PIXIE_TRAIL;
            	
            	switch (this.getPixieType())
            	{
            		case 0:
            		default:
            			particle = FFParticleTypes.PINK_PIXIE_TRAIL;
            			break;
            			
            		case 1:
            			particle = FFParticleTypes.BLUE_PIXIE_TRAIL;
            			break;
            	
            		case 2:
            			particle = FFParticleTypes.PURPLE_PIXIE_TRAIL;
            			break;
            	}
            	
            	FamiliarFauna.proxy.spawnParticle(particle, this.world, this.posX + (this.rand.nextDouble() - 0.5D) * (double)this.width, this.posY + this.rand.nextDouble() * (double)this.height, this.posZ + (this.rand.nextDouble() - 0.5D) * (double)this.width, 0.0D, 0.0D, 0.0D, new int[0]);
            }
        }
    }
        
    @Override
    public boolean canBePushed()
    {
        return false;
    }

    @Override
    protected void collideWithEntity(Entity entityIn)
    {
    }

    @Override
    protected void collideWithNearbyEntities()
    {
    }
    
    @Override
    protected boolean canTriggerWalking()
    {
        return false;
    }
    
    @Override
    public void fall(float distance, float damageMultiplier)
    {
    }

    @Override
    protected void updateFallState(double y, boolean onGroundIn, IBlockState state, BlockPos pos)
    {
    }

    @Override
    public boolean doesEntityNotTriggerPressurePlate()
    {
        return true;
    }
    
    @Override
    public boolean isOnLadder()
    {
        return false;
    }
    
    @Override
    public void onUpdate()
    {
        super.onUpdate();

        if (!this.world.isRemote && (!(ConfigurationHandler.pixieEnable)))
        {
            this.setDead();
        }
    }
    
    @Override
    public boolean getCanSpawnHere()
    {
    	BlockPos blockpos = new BlockPos(this.posX, this.getEntityBoundingBox().minY, this.posZ);

        if (blockpos.getY() <= this.world.getSeaLevel())
        {
            return false;
        }
        else
        {
        	if (blockpos.getY() >= 90)
	        {
	            return false;
	        }
        	else
        	{
	        	int light = this.world.getLightFromNeighbors(blockpos);
	        	
	        	return light > 8 && super.getCanSpawnHere();
        	}
        }
    }
    
    @Override
    public void travel(float strafe, float vertical, float forward)
    {
        if (this.isInWater())
        {
            this.moveRelative(strafe, vertical, forward, 0.02F);
            this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.800000011920929D;
            this.motionY *= 0.800000011920929D;
            this.motionZ *= 0.800000011920929D;
        }
        else if (this.isInLava())
        {
            this.moveRelative(strafe, vertical, forward, 0.02F);
            this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.5D;
            this.motionY *= 0.5D;
            this.motionZ *= 0.5D;
        }
        else
        {
            float f = 0.91F;

            if (this.onGround)
            {
                BlockPos underPos = new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(this.getEntityBoundingBox().minY) - 1, MathHelper.floor(this.posZ));
                IBlockState underState = this.world.getBlockState(underPos);
                f = underState.getBlock().getSlipperiness(underState, this.world, underPos, this) * 0.91F;
            }

            float f1 = 0.16277136F / (f * f * f);
            this.moveRelative(strafe, vertical, forward, this.onGround ? 0.1F * f1 : 0.02F);
            f = 0.91F;

            if (this.onGround)
            {
                BlockPos underPos = new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(this.getEntityBoundingBox().minY) - 1, MathHelper.floor(this.posZ));
                IBlockState underState = this.world.getBlockState(underPos);
                f = underState.getBlock().getSlipperiness(underState, this.world, underPos, this) * 0.91F;
            }

            this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
            this.motionX *= (double)f;
            this.motionY *= (double)f;
            this.motionZ *= (double)f;
        }

        this.prevLimbSwingAmount = this.limbSwingAmount;
        double d1 = this.posX - this.prevPosX;
        double d0 = this.posZ - this.prevPosZ;
        float f2 = MathHelper.sqrt(d1 * d1 + d0 * d0) * 4.0F;

        if (f2 > 1.0F)
        {
            f2 = 1.0F;
        }

        this.limbSwingAmount += (f2 - this.limbSwingAmount) * 0.4F;
        this.limbSwing += this.limbSwingAmount;
    }
    
    // Helper class representing a point in space that the pixie is targeting for some reason
    class PixieMoveTargetPos
    {
        private EntityPixie pixie = EntityPixie.this;

        public double posX;
        public double posY;
        public double posZ;
        public double distX;
        public double distY;
        public double distZ;
        public double dist;
        public double aimX;
        public double aimY;
        public double aimZ;
        
        public PixieMoveTargetPos()
        {
            this(0, 0, 0);
        }
        
        public PixieMoveTargetPos(double posX, double posY, double posZ)
        {
            this.setTarget(posX, posY, posZ);
        }
        
        public void setTarget(double posX, double posY, double posZ)
        {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.refresh();
        }
        
        public void refresh()
        {
            this.distX = this.posX - this.pixie.posX;
            this.distY = this.posY - this.pixie.posY;
            this.distZ = this.posZ - this.pixie.posZ;
            
            this.dist = (double)MathHelper.sqrt(this.distX * this.distX + this.distY * this.distY + this.distZ * this.distZ);
            
            // (aimX,aimY,aimZ) is a unit vector in the direction we want to go
            if (this.dist == 0.0D)
            {
                this.aimX = 0.0D;
                this.aimY = 0.0D;
                this.aimZ = 0.0D;
            }
            else
            {
                this.aimX = this.distX / this.dist;
                this.aimY = this.distY / this.dist;
                this.aimZ = this.distZ / this.dist;                
            }
         }
        
        public boolean isBoxBlocked(AxisAlignedBB box)
        {
            return !this.pixie.world.getCollisionBoxes(this.pixie, box).isEmpty();
        }
        
        // check nothing will collide with the pixie in the direction of aim, for howFar units (or until the destination - whichever is closer)
        public boolean isPathClear(double howFar)
        {
            howFar = Math.min(howFar, this.dist);
            AxisAlignedBB box = this.pixie.getEntityBoundingBox();
            for (double i = 0.5D; i < howFar; ++i)
            {
                // check there's nothing in the way
                if (this.isBoxBlocked(box.offset(this.aimX * i, this.aimY * i, this.aimZ * i)))
                {
                    return false;
                }
            }
            return !this.isBoxBlocked(box.offset(this.aimX * howFar, this.aimY * howFar, this.aimZ * howFar));
        }
        
    }
            
    class PixieMoveHelper extends EntityMoveHelper
    {
        // EntityMoveHelper has the boolean 'update' which is set to true when the target is changed, and set to false when a bearing is set
        // So it means 'the target has changed but we're not yet heading for it'
        // We'll re-use it here with a slightly different interpretation
        // Here it will mean 'has a target and not yet arrived'
        
        private EntityPixie pixie = EntityPixie.this;
        private int courseChangeCooldown = 0;
        private double closeEnough = 0.3D;
        private PixieMoveTargetPos targetPos = new PixieMoveTargetPos();

        public PixieMoveHelper()
        {
            super(EntityPixie.this);
        }
                        
        @Override
        public void setMoveTo(double x, double y, double z, double speedIn)
        {
            super.setMoveTo(x,y,z,speedIn);
            this.targetPos.setTarget(x, y, z);
        }

        @Override
        public void onUpdateMoveHelper()
        {
            // if we have arrived at the previous target, or we have no target to aim for, do nothing
            if (this.action != Action.MOVE_TO) {return;}
            
            if (this.courseChangeCooldown-- > 0) {
                // limit the rate at which we change course
                return;
            }
            this.courseChangeCooldown += this.pixie.getRNG().nextInt(2) + 2;
            
            // update the target position
            this.targetPos.refresh();
            
            // accelerate the pixie towards the target
            double acceleration = 0.1D;
            this.pixie.motionX += this.targetPos.aimX * acceleration;
            this.pixie.motionY += this.targetPos.aimY * acceleration;
            this.pixie.motionZ += this.targetPos.aimZ * acceleration;
           
            // rotate to point at target
            this.pixie.renderYawOffset = this.pixie.rotationYaw = -((float)Math.atan2(this.targetPos.distX, this.targetPos.distZ)) * 180.0F / (float)Math.PI;            

            // abandon this movement if we have reached the target or there is no longer a clear path to the target
            if (!this.targetPos.isPathClear(5.0D))
            {
                //System.out.println("Abandoning move target - way is blocked" );
                this.action = Action.WAIT;
            } else if (this.targetPos.dist < this.closeEnough) {
                //System.out.println("Arrived (close enough) dist:"+this.targetPos.dist);
                this.action = Action.WAIT;
            }
        }        

    }
    
    // AI class for implementing the random flying behaviour
    class AIPixieRandomFly extends EntityAIBase
    {
        private EntityPixie pixie = EntityPixie.this;
        private PixieMoveTargetPos targetPos = new PixieMoveTargetPos();
        
        public AIPixieRandomFly()
        {
            this.setMutexBits(1);
        }

        // should we choose a new random destination for the pixie to fly to?
        // yes, if the pixie doesn't already have a destination
        @Override
        public boolean shouldExecute()
        {
            return !this.pixie.getMoveHelper().isUpdating();
        }
        
        @Override
        public boolean shouldContinueExecuting() {return false;}
        
        // choose a a new random destination for the pixie to fly to
        @Override
        public void startExecuting()
        {            
            Random rand = this.pixie.getRNG();
            // pick a random nearby point and see if we can fly to it
            if (this.tryGoingRandomDirection(rand, 6.0D)) {return;}
            // pick a random closer point to fly to instead
            if (this.tryGoingRandomDirection(rand, 2.0D)) {return;}
            // try going straight along axes (try all 6 directions in random order)
            List<EnumFacing> directions = Arrays.asList(EnumFacing.values());
            Collections.shuffle(directions);
            for (EnumFacing facing : directions)
            {
                if (this.tryGoingAlongAxis(rand, facing, 1.0D)) {return;}
            }
        }
        
        
        // note y direction has a slight downward bias to stop them flying too high
        public boolean tryGoingRandomDirection(Random rand, double maxDistance)
        {
            double dirX = ((rand.nextDouble() * 2.0D - 1.0D) * maxDistance);
            double dirY = ((rand.nextDouble() * 2.0D - 1.1D) * maxDistance);
            double dirZ = ((rand.nextDouble() * 2.0D - 1.0D) * maxDistance);
            return this.tryGoing(dirX, dirY, dirZ);
        }
        
        public boolean tryGoingAlongAxis(Random rand, EnumFacing facing, double maxDistance)
        {
            double dirX = 0.0D;
            double dirY = 0.0D;
            double dirZ = 0.0D;
            switch (facing.getAxis())
            {
                case X:
                    dirX = rand.nextDouble() * facing.getAxisDirection().getOffset() * maxDistance;
                    break;
                case Y:
                    dirY = rand.nextDouble() * facing.getAxisDirection().getOffset() * maxDistance;
                    break;
                case Z: default:
                    dirZ = rand.nextDouble() * facing.getAxisDirection().getOffset() * maxDistance;
                    break;
            }
            return this.tryGoing(dirX, dirY, dirZ);
        }
        
        public boolean tryGoing(double dirX, double dirY, double dirZ)
        {
            //System.out.println("("+dirX+","+dirY+","+dirZ+")");
            this.targetPos.setTarget(this.pixie.posX + dirX, this.pixie.posY + dirY, this.pixie.posZ + dirZ);
            //System.out.println("Testing random move target distance:"+this.targetPos.dist+" direction:("+this.targetPos.aimX+","+this.targetPos.aimY+","+this.targetPos.aimZ+")");
            boolean result = this.targetPos.isPathClear(5.0F);
            if (result)
            {
                this.pixie.getMoveHelper().setMoveTo(this.targetPos.posX, this.targetPos.posY, this.targetPos.posZ, 1.0D);
            }
            return result;
        }
    }
}
