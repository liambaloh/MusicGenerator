MeusicGenerator
===============

Music phrase generator.

Demonstration video:
http://www.youtube.com/watch?v=EzGlyDgAtBU

Credits:
- Matevž Baloh (https://github.com/errorage)
- You are welcome to add your name and github account here if you contribute to this repo

How the programs work:
- I started the project from a midi streamer (Proved to not ba a good idea)
- The streamer is the program that actually generates the sequence and sends the midi messages. (only one can be ran at once, since each connects to the player program through the same port)
- The player is the program that receives midi messages and actually plays them. (Must be running when streamer is started to hear anything)

How to use: 
- Open in eclipse
- Start MIDIKeyboardStreamPlayer as a Java application
- Start MIDIKeyboardStreamer as a Java applet
- Select MIDIKeyboardStreamer's text input field
- Press the 'w' button on your keyboard to start the generator. (Yes, I realize this is not intuitive, it's this way due to the development process - "because it happened")
- You can also press any other button to play that letter's ascii key value tone. Press to initiate the tone, release to stop it (ensure you have the MIDIKeyboardStreamer's input field selected tho.).
- You can change the program's behavior so it generates various types of chords when you press each button by selecting the chord type, which you do by pressing the 0 - 9 keys. 0 = single tone; 1 = major triad; 2 = minor triad; 3 = augmented triad; 4 = diminished triad, followed by seventh chords, etc. Look at sendChord(..);
- The button 'q' plays a chord sequence of wholenotes, which is completely determined by the basic chord progression markov chain.

Note: The following text refers to note durations as they are interpreted code-wise. The actual melody plays twice slower, so a wholenote sounds like a half note, and a sixteenth note (shortest supported note) sounds like an eigth note. So remember that the explanation below actually 'sounds' twice slower when played. It's a bug which would take too long to root out of code, sorry about that. (you're welcome to rename everything so it makes sense tho! :) )

How tone generation works (music theory):
- Works in three stages:
  - Generates chord progression which makes sense for a music phrase
  - Generates a melody of sixteenth notes
  - Alters the generated melody with rhytm, making the final product

- Chord progresssion:
  - Based on a markov model, described in allowedProgression and allowedProgressionProb (press 'q' in streamer to generate a progression based solely on the model) - see generateChordProgression()
  - The music phrase generation is a mostly hardcoded sequence, which essentially breaks up the basic chord progression markov model into a series of steps. Technically it could all be a single markov model, but it would not make much sense to do that. (the same state in various stages of the phrase would not behave the same way) - see generateOpeningPhraseChordProgression():
  - The phrase is made up of 8 bars.
  - The phrase is made of two statements (accusation and response), each with 4 bars.
  - The accusation statement is made of 4 bars, 2 belonging to the basic 'theme', 1 to the secondary theme, and the last 1 is a half cadence.
  - The response statement is made of 4 bars, 2 are the same as the first two in the accusation statement (theme repeat), 1 to a tertiary theme, and the last 1 is a perfect cadence.
  - See generateOpeningPhraseChordProgression() for full sequence.

- Melody generation:
  - It is only important that the melody sounds good with the chord progression at the time the chord actually kicks in. What the melody does after that will not clash with the chord. The melody's tone which is played at the time the chord plays is (in the program) called the 'start tone'.
  - What the start tone will be is determined by probabilities, which are determined by which note in the scale the chord progression is at. These probabilities are held in 'chordStartToneProbabilities'. At the time of writing, all of them are set to {0.4f, 0.1f, 0.1f, 0f, 0.25f, 0.05f, 0.1f}). If the chord at the time is a IV chord, the chance of the melody's start tone being IV is 40%, of it being V is 10%, vi is 10%, etc. A triad with the fourth tone playing (So tone 1, 3, 4 and 5 playing at the same time) doesn't sound good by itself, and doesn't want to resolve to anything. (1, 2, 3, 5 does resolve; as does 1, 3, 5, 6 and 1, 3, 5, 7 (seventh chord))
  - After the start tone is determined, the melody will randomly move up by one, down by one or stay at the same tone. This tends to create something listenable. It is obviously not going to create anything 'daring', but will generally create acceptable melodies. At this stage, it still creates a tone for each eigth note space - rhythm is not determined yet.
  - Some segments of the melody are overwritten. Specifically the end of the half cadence is forced to a dotted halfnote on V, and the end of the perfect cadence is forced to a dotted halfnote on I. This is because the two cadences end their respective statements, and a longer tone needs to be there so it sounds like an ending.

- Rhythm generation:
  - Extremely simple. Basically it runs through the entire melody sequence of sixteenth notes, and with a chance of 40% it extends it to an eigth note. This eigth note overrides whatever was generated as the second sixteenthnote half of this eigthnote. So in the sequence of sixteenth notes (midi note values): 60, 62, 64, 65, 67; if the note with the value 62 was extended into an eigth note, the new eigth note would override the sixteenth note would override the sixteenth note with the value 64. The sequence would thus be 60, 62-62, 65, 67; (the dash represents a continuation - an eight note)
  - The only condition is that the extension can't happen at the fourth sixteenth note in a quarternote. This is because it would extend past the quarternote it starts in. So the sequence: X X X X (X = sixteenth note period, - = continuation) can validly be changed to X-X X X or X X-X X or X-X X-X (among many others), but it can't be changed into X X X X- (with the - extending into a sixteenth note that is not part of this quarternote period). This is called sincopation, and is definitely not forbidden in 'proper' music composition. But the rules of sincopation were not programmed into the application, so it made more sense to not allow it. 

