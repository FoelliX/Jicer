![Java 17](https://img.shields.io/badge/java-17-brightgreen.svg) ![Maven 3.8.1](https://img.shields.io/badge/maven-3.8.1-brightgreen.svg)
---
<p align="center">
	<img src="https://FoelliX.github.io/Jicer/logo.png" width="300px"/>
</p>

# Jicer
Jicer is a static Jimple slicer.
It is especially designed for slicing Android apps.
Since it takes APK files as input and also outputs slices as APK files, it can smoothly be integrated into cooperative analyses such as [CoDiDroid](https://github.com/FoelliX/CoDiDroid).
The use of Jicer in a cooperative analysis is also explained and demonstrated in [tutorial video #03](https://github.com/FoelliX/AQL-System/wiki/Video_tutorials#user-content-video-03-reduce-false-positives-i-slicing) of the [AQL-System](https://github.com/FoelliX/AQL-System).

## Introduction/Tutorial Video
The video below shows how to configure and use Jicer:
[![Video](https://FoelliX.de/videos/tutorials/Jicer/splash.png)](https://FoelliX.de/videos/tutorials/Jicer/video_00.mp4)

**Material:**
- Android platforms directory repository [[Link]](https://github.com/Sable/android-platforms)
- Example app `debugJicerRunEx_new.apk` [[Download]](https://github.com/FoelliX/Jicer/blob/main/example/debugJicerRunEx_new.apk)
- Input edges or ICC analysis result `debugJicerRunExInputEdges.xml` (in [AQL](https://github.com/FoelliX/AQL-System/wiki/AQL) format) [[Download]](https://github.com/FoelliX/Jicer/blob/main/example/debugJicerRunExInputEdges.xml)

## GUI
To open the GUI simply run: `java -jar Jicer-X.X.X.jar -gui`  

[![Screenshot](https://FoelliX.github.io/Jicer/gui.png)](https://FoelliX.github.io/Jicer/gui.png)

All launch parameters can also be configured via the GUI.

## Launch Parameters
The following table shows all the available launch parameters.

|Parameter|Meaning|
|---------------------------|------------------------------------------------------|
| -gui				| If this parameter is given, the GUI will be launched. Any other parameter will be used to select the initial options in the GUI. |
| -mode , -m		| Three modes can be chosen: ``slice``, ``sliceout`` and ``show``. In ``slice``-mode only the elements in the slice will be kept in the output. The opposite is the case in ``sliceout``-mode: whatever is in the slice is removed from the output. The last mode ``show`` does not slice the output - whatever belongs to the slice is only shown in the log. |
| -from , -to		| One or two slicing criterion must be provided. ```` refers to the forward slicing criterion, whereas ```` refers to the backward criterion. Both must be provided in AQL format. Here is an example: ``-from "Statement('$r4 = virtualinvoke $r3.()')-&gt;Method('')-&gt;Class('de.foellix.aql.MainActivity')-&gt;App('...\App.apk')"`` (see next parameter for simpler input). |
| -simpleinput, -simple, -si | Allows to use simple input. For example instead of a full Jimple statement (``Statement('$r4 = virtualinvoke $r3.()')``) only the called method name can be used: ``Statement('getDeviceId')`` |
| -d , -debug		| The output generated during the execution of this tool can be set to different levels. ```` may be set to: "error", "warning", "normal", "debug", "detailed", "verbose" (ascending precision from left to right). Additionally it can be set to "short", the output will then be equal to "normal" but shorter at some points. By default it is set to "normal". |
| -dg, -draw, -drawGraphs | Enables ADG output in form of an SVG (``./data/temp/graphs/sdg_slicing.svg``) |
| -f, -format		| The following four output formats can be used: apk, jimple, class, none. Class can only be used when the input is an class (e.g. ``App('.../A.class')``). |
| -o , -out , -output | By default the output file is created in the same directory where Jicer is run. The output file has the same name as the input file. Via this parameter a different path and filename can be specified. |
| -ie , -inputEdges	| An [AQL-Answer](https://github.com/FoelliX/AQL-System/wiki/Answers) can be given to Jicer to enhance the ADG - ```` refers to such an answer. |
|  -ra, -run, -runnable | By setting the parameter the created output contains statements required to run the app. |
| -s, -sign			| Implies the parameter above. The output app will be signed as specified in ``config.properties``. |
| -i, -in, -incomplete | The slice created is as small as possible. In most scenarios an incomplete slice is neither analyzable nor runnable. These slices are suited best for debugging. |
| -nff				| Forward field filtering (FFF) can be deactivated with this parameter. |
| -ncsr				| Context-sensitive refinement (CSR) can be deactivated with this parameter. |
| -sts				| Prefer local data (PLD) can be switched to strict thread-sensitivity (STS) by this parameter. |
| -os, -overapproximateSummaries | If StubDroid cannot provide sufficient information for a method, this parameter decides what is assumed. By default it is assumed that the method does not assign anything to parameters or the method call's base. When this parameter is given the opposite is assumed. |
| -k , -limit , -klimit , -k-limit | Maxmimal execution steps of the reaching definition analysis. Whenever this limit is reached Jicer provides a warning (Default: 100,000). |
| -eol, -excludeOrdinaryLibraries | The libraries configured in ``config.properties`` will be excluded (not loaded). This may affect slicing accuracy but also boosts performance. |
| -sol, -sliceOrdinaryLibraries | Automatically deactivates ``-eol``. Makes Jicer slice through the libraries specified in ``config.properties``. |
| -ns, -nostats, -nostatistics | Disables logging stats about the ADG and the slicing process. |

## Features
In the following we present a list of features that make Jicer support multiple slicing use-cases and different slice granularities:
- APK/class input
- APK/Jimple output
- ADG Generation
    - flow-, context-, field-, object- and thread-sensitive
    - Callback- &amp; Lifecycle-aware
- Scalable w.r.t. libraries (through StubDroid summaries)
- ICC &amp; IAC support via cooperative analysis (input edges)
- *Debugable*, *Analyzable* or *Executable* output
- Valid code slicing through extra-slicing
- Forward Field Filtering
- Context-Sensitive Refinement
- Prefer Local Data
- Call Graph Enhancing
- ... and much more

## Publications
- Jicer: Simplifying Cooperative Android App Analysis Tasks (Felix Pauck, Heike Wehrheim)  
[SCAM 2021](https://www.ieee-scam.org/2021) - [https://ieeexplore.ieee.org/document/9610738](https://ieeexplore.ieee.org/document/9610738)

### Presentation
The slides and video as presented during SCAM 2021 are also available:
- [Slides](https://foellix.de/videos/presentations/SCAM21/Jicer.pdf)
- [Video](https://foellix.de/videos/presentations/SCAM21/Jicer.mp4)

### Artifact
The reviewed artifact is available at Zenodo:
[https://zenodo.org/record/5462859](https://zenodo.org/record/5462859)

### Evaluation - Results
The results associated with the three experiments can be obtained here:
- RQ2: [here](https://my.hidrive.com/lnk/AOC5r3pH)
- RQ3: [here](https://my.hidrive.com/lnk/vcCZr0C0)
- RQ4: [here](https://my.hidrive.com/lnk/x1CZraYr)

## License
The AQL-System is licensed under the *GNU General Public License v3* (see [LICENSE](https://github.com/FoelliX/AQL-System/blob/master/LICENSE)).

# Contact
**Felix Pauck** (FoelliX)  
Paderborn University  
fpauck@mail.uni-paderborn.de  
[http://www.FelixPauck.de](http://www.FelixPauck.de)

# Links
- BREW has been used to carry out evaluations of cooperative analyses that include Jicer: [https://github.com/FoelliX/BREW](https://github.com/FoelliX/BREW)
- BREW is based on the AQL-System: [https://github.com/FoelliX/AQL-System](https://github.com/FoelliX/AQL-System)
- one cooperative analysis also mentioned in the paper is CoDiDroid: [https://github.com/FoelliX/CoDiDroid](https://github.com/FoelliX/CoDiDroid)