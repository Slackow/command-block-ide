package arm32x.minecraft.commandblockide.gui;

import arm32x.minecraft.commandblockide.Dirtyable;
import arm32x.minecraft.commandblockide.update.DataCommandUpdateRequester;
import java.util.stream.Stream;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.UpdateCommandBlockC2SPacket;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.CommandBlockExecutor;

public final class CommandBlockEditor extends CommandEditor implements Dirtyable {
	private CommandBlockBlockEntity blockEntity;

	private final TextFieldWidget lastOutputField;

	private final CommandBlockTypeButton typeButton;
	private final CommandBlockAutoButton autoButton;
	private final CommandBlockTrackOutputButton trackOutputButton;

	private boolean dirty = false;

	public CommandBlockEditor(Screen screen, TextRenderer textRenderer, int x, int y, int width, int height, CommandBlockBlockEntity blockEntity, int index) {
		super(screen, textRenderer, x, y, width, height, 40, 24, index);
		this.blockEntity = blockEntity;

		lastOutputField = new TextFieldWidget(textRenderer, commandField.x, commandField.y, commandField.getWidth(), commandField.getHeight(), new TranslatableText("advMode.previousOutput").append(new TranslatableText("commandBlockIDE.narrator.editorIndex", index + 1)));
		lastOutputField.setEditable(false);
		lastOutputField.setMaxLength(32500);
		lastOutputField.setText(new TranslatableText("commandBlockIDE.unloaded").getString());
		lastOutputField.visible = false;

		typeButton = addButton(new CommandBlockTypeButton(screen, x + 20, y));
		typeButton.type = blockEntity.getCommandBlockType();
		typeButton.active = false;

		autoButton = addButton(new CommandBlockAutoButton(screen, x + 40, y));
		autoButton.auto = typeButton.type == CommandBlockBlockEntity.Type.SEQUENCE;
		autoButton.active = false;

		trackOutputButton = addButton(new CommandBlockTrackOutputButton(screen, x + width - 20, y));
		trackOutputButton.trackingOutput = true;
		trackOutputButton.active = false;
	}

	@Override
	public void apply(ClientPlayNetworkHandler networkHandler) {
		if (isLoaded() && Stream.<Dirtyable>of(this, typeButton, autoButton, trackOutputButton).anyMatch(Dirtyable::isDirty)) {
			CommandBlockExecutor executor = blockEntity.getCommandExecutor();
			networkHandler.sendPacket(new UpdateCommandBlockC2SPacket(
				new BlockPos(executor.getPos()),
				commandField.getText(),
				typeButton.type,
				trackOutputButton.trackingOutput,
				typeButton.conditional,
				autoButton.auto
			));
			executor.shouldTrackOutput(trackOutputButton.trackingOutput);
			if (!trackOutputButton.trackingOutput) {
				executor.setLastOutput(null);
			}
		}
	}

	@Override
	public void update() {
		CommandBlockExecutor executor = blockEntity.getCommandExecutor();
		commandField.setText(executor.getCommand());
		typeButton.type = blockEntity.getCommandBlockType();
		typeButton.conditional = blockEntity.isConditionalCommandBlock();
		autoButton.auto = blockEntity.isAuto();
		trackOutputButton.trackingOutput = executor.isTrackingOutput();

		String lastOutput = executor.getLastOutput().getString();
		if (lastOutput.equals("")) {
			lastOutput = new TranslatableText("commandBlockIDE.lastOutput.none").getString();
		}
		lastOutputField.setText(lastOutput);

		this.commandField.setEditable(true);
		typeButton.active = true;
		autoButton.active = true;
		trackOutputButton.active = true;
		suggestor.setWindowActive(commandField.isActive());
		suggestor.refresh();

		dirty = false;
		setLoaded(true);
	}

	@Override
	public void requestUpdate(ClientPlayNetworkHandler networkHandler) {
		DataCommandUpdateRequester.getInstance().requestUpdate(networkHandler, blockEntity);
	}

	@Override
	public void commandChanged(String newCommand) {
		if (!newCommand.equals(blockEntity.getCommandExecutor().getCommand())) {
			markDirty();
		}
		super.commandChanged(newCommand);
	}

	@Override
	protected void renderCommandField(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		if (trackOutputButton.isMouseOver(mouseX, mouseY)) {
			commandField.visible = false;
			lastOutputField.visible = true;
			lastOutputField.render(matrices, mouseX, mouseY, delta);
		} else {
			commandField.visible = true;
			lastOutputField.visible = false;
			commandField.render(matrices, mouseX, mouseY, delta);
		}
	}

	@Override
	public boolean isDirty() { return dirty; }

	@Override
	public void markDirty() { dirty = true; }

	@Override
	public void setY(int y) {
		super.setY(y);

		lastOutputField.y = commandField.y;

		typeButton.y = y;
		autoButton.y = y;
		trackOutputButton.y = y;
	}

	@Override
	public void setWidth(int width) {
		super.setWidth(width);

		lastOutputField.setWidth(commandField.getWidth());

		trackOutputButton.x = getX() + width - 20;
	}
}
