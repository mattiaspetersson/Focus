// ===========================================================================
// Title         : Focus
// Description   :
//               | for live-electronic ensemble by Mattias Petersson, 2019
//
// Requires the Utopia quark. Run:
// Quarks.install("https://github.com/muellmusik/Utopia")
// ===========================================================================

Focus {

	var name, numPlayers, numCards, restProbability, timeUnit, fontSize, repeats;
	var win, cards, strategy, clearedCards, tempoClock;
	var hail, <peers, peerNames, <scrambledPeerArray;

	*new {|name = 'yourNameHere', numPlayers = 9, numCards = 1, restProbability = 0.1, timeUnit = 9, fontSize = 18, repeats = 1|
		^super.newCopyArgs(name, numPlayers, numCards, restProbability, timeUnit, fontSize, repeats).initFocus;
	}

	initFocus {
		cards = numCards.collect{|i| FocusCard(fontSize)};
		strategy = Pn(Pshuf(this.strategies)).asStream;
		tempoClock = TempoClock.default;
		tempoClock.tempo = 1;

		hail = Hail(me: Peer(name.asSymbol, NetAddr.localAddr), oscPath: '/focus');
		peers = hail.addrBook;
		peerNames = peers.names; // returns a Set

		OSCdef(name.asSymbol, {|msg|
			msg.removeAt(0);
			scrambledPeerArray = msg;
			scrambledPeerArray.postln;
		}, '/newPeerArray');

		this.makeWindow(4); // spacing in pixels
		this.scramblePeerArray;
	}

	scramblePeerArray { // makes sure everyone has the same array of names
		scrambledPeerArray = peers.names.asArray.scramble;
		peers.sendAll('/newPeerArray', *scrambledPeerArray);
	}

	obliquePlayer {
		var urn = Pn(Pshuf(Array.series(numPlayers))).asStream;
		clearedCards = [];

		^Task {
			// intro (fade in cards)
			cards.do{|c| c.flash(3*timeUnit, 'up')};
			(4*timeUnit).wait;

			repeats.do{
				// new cards for each player one by one
				scrambledPeerArray.do{|n|
					if(n == name) {
						defer {
							this.updateCard(urn.next);
						};
						timeUnit.wait;
					};
				};
				/*numPlayers.do{|i|
					defer {
						this.updateCard(urn.next);
					};
					timeUnit.wait;
				};*/
				timeUnit.wait;

				// new cards for everyone
				defer {
					cards.size.do{|i| this.updateCard(i)};
				};
				(7*timeUnit).wait;

				// new cards for everyone
				defer {
					cards.size.do{|i| this.updateCard(i)};
				};
				//(1*timeUnit).wait;
				//cards.do{|c| c.flash(8*timeUnit, 'up')};
				(9*timeUnit).wait;

				// new cards for each player one by one
				numPlayers.do{|i|
					defer {
						this.updateCard(urn.next);
					};
					timeUnit.wait;
				};

				// new cards for everyone
				defer {
					cards.size.do{|i| this.updateCard(i)};
				};
				(9*timeUnit).wait;
			};

			// clear cards for each player one by one
			numPlayers.do{|i|
				defer {
					this.clearCard(urn.next);
				};
				timeUnit.wait;
			};

			// fade to grey
			defer {
				cards.do{|c| c.fadeOut(9, win.background)};
			};
		};
	}

	updateCard {|i, flashTime = 3.6|
		if(restProbability.coin, {
			cards[i].flash(flashTime).string_("Rest");
		}, {
			cards[i].flash(flashTime).string_(strategy.next);
		});
	}

	clearCard {|i|
		cards[i].flash.string_("");
		if(clearedCards.size > 0, {cards[clearedCards.choose].flash});
		clearedCards = clearedCards.add(i);
	}

	makeWindow {|space|
		var obPlayer, grid, for, by;
		case {numCards == 1} {for = 1; by = 1};
		case {numCards == 4} {for = 2; by = 2};
		case {numCards == 8} {for = 4; by = 2};
		case {numCards == 9} {for = 3; by = 3};
		case {numCards == 12} {for = 4; by = 3};
		case {numCards == 16} {for = 3; by = 4};

		win = Window("Focus");
		grid = GridLayout();

		cards.do{|c, i|
			grid.add(c.view, i div: by, i%for) //3 here
		};
		for.do({|r| grid.setRowStretch(r, 1)}); //and here
		by.do({|c| grid.setColumnStretch(c, 1)}); //and here for 3 by 3
		win.view.keyDownAction_(
			{|... args|
				var view, char, modifiers, unicode, keycode;
				#view, char, modifiers, unicode, keycode = args;
				switch(unicode,
					32, {//space starts or pauses
						if(obPlayer.isNil,
							{obPlayer = this.obliquePlayer.start},
							{
								if(obPlayer.isPlaying.not,
									{obPlayer.play},
									{obPlayer.pause}
								);
							}
						);
					},
					27, {//esc stops and resets
						obPlayer.stop;
						cards.do{|c|
							c.fader.stop;
							c.string_("");
							c.view.background_(c.cardClr)
						};
						obPlayer = nil;
					},
					70, {win.fullScreen}, //shift+f
					102, {win.endFullScreen} //f
				);
			}
		);
		win.background_(Color.grey(0.2));
		win.layout_(
			grid.vSpacing_(space).hSpacing_(space).margins_(space)
		);
		win.onClose_({obPlayer.stop});
		win.front;
	}

	strategies {
		^[
			"Initiative",
			"Unison",
			"Affect",
			"Differences",
			"Similarities",
			"Mistake",
			"Repetition",
			"Opposites",
			"Harmony",
			"Rhythm",
			"Melody",
			"Silence",
			"Inwards",
			"Outwards",
			"Construction",
			"Destruction",
			"Simplicity",
			"Complexity",
			"Awakeness",
			"Openness",
			"Mimicry",
			"Risk",
			"Fear",
			"Memory"
		]
	}
}

FocusCard {
	var card, <cardClr, cardSize, flashClr, <fader;
	var font, text;

	*new {|fontSize = 18, cardSize = 300|
		^super.new.initFocusCard(fontSize, cardSize)
	}

	initFocusCard {|fontSize, size|
		cardClr = Color.white;
		flashClr = Color.yellow;
		//cardSize = Size(size, size*0.618); // fix card size so it adapts to the window resolution
		font = Font("Menlo", fontSize);
		card = View().background_(cardClr); //.fixedSize_(cardSize);
		text = StaticText().font_(font).align_(\center).stringColor_(Color.black);
		card.layout_(HLayout(text));
	}

	string_ {|aString|
		text.string_(aString);
	}

	view {
		^card;
	}

	flash {|fadeTime = 1.8, direction = \down|
		var fade, waitTime = 1/60, env;

		env = switch(direction,
			\down, {Env([1, 0], [fadeTime], \sine).asStream},
			\up, {Env([0, 1], [fadeTime],\sine).asStream}
		);
		fader = Routine {
			inf.do {|i|
				fade = i/fadeTime * waitTime;
				if(fade>1, {thisThread.stop});
				card.background_(cardClr.blend(flashClr, env.next));
				waitTime.wait;
			}
		}.play(AppClock);
	}

	fadeOut {|fadeTime = 9, fadeToColor|
		var fade, waitTime = 1/90, env;

		env = Env([1, 0], [fadeTime], \sine).asStream;
		fader = Routine {
			inf.do {|i|
				fade = i/fadeTime * waitTime;
				if(fade>1, {thisThread.stop});
				card.background_(fadeToColor.blend(cardClr, env.next));
				waitTime.wait;
			}
		}.play(AppClock);
	}
}