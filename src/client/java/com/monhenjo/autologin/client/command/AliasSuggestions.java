package com.monhenjo.autologin.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.monhenjo.autologin.AutoLogin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Puts the saved labels into the suggestion popup of the server's own {@code /login},
 * {@code /l}, {@code /register} and {@code /reg} commands.
 *
 * <p>Registering client commands with those names is not enough: the server's command tree is
 * merged first and keeps the node, so the popup keeps showing whatever the plugin offered (usually
 * nothing). The only place to add suggestions is that node, so its argument child is rebuilt with
 * the same name and type and a suggestion provider of ours. Parsing and execution are untouched -
 * the client only ever uses this tree to show completions, and anything typed is still sent to the
 * server, which parses it with its own copy.
 */
public final class AliasSuggestions {
	private static final String[] VERBS = {"login", "l", "register", "reg"};

	private AliasSuggestions() {
	}

	public static void attach(CommandDispatcher<?> dispatcher, Supplier<List<String>> labels) {
		if (dispatcher == null) {
			return;
		}

		RootCommandNode<?> root = dispatcher.getRoot();

		for (String verb : VERBS) {
			CommandNode<?> node = root.getChild(verb);
			if (!(node instanceof LiteralCommandNode<?> literal)) {
				continue;
			}

			try {
				replaceArguments(literal, labels);
			} catch (RuntimeException e) {
				// A plugin can register an argument shape we cannot copy; leave its node alone.
				AutoLogin.LOGGER.debug("autologin: could not add suggestions to /{}: {}", verb, e.toString());
			}
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void replaceArguments(LiteralCommandNode<?> literal, Supplier<List<String>> labels) {
		List<CommandNode> children = new ArrayList<>((List) literal.getChildren());

		for (CommandNode child : children) {
			if (!(child instanceof ArgumentCommandNode argument)) {
				continue;
			}

			SuggestionProvider provider = (context, builder) -> {
				String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);

				for (String label : labels.get()) {
					if (label.toLowerCase(Locale.ROOT).startsWith(prefix)) {
						builder.suggest(label);
					}
				}

				return builder.buildFuture();
			};

			RequiredArgumentBuilder builder = RequiredArgumentBuilder.argument(argument.getName(), argument.getType());
			builder.suggests(provider);
			builder.requires(argument.getRequirement());

			CommandNode rebuilt = builder.build();
			for (CommandNode grandchild : (Iterable<CommandNode>) argument.getChildren()) {
				rebuilt.addChild(grandchild);
			}

			literal.addChild(rebuilt);
		}
	}
}
