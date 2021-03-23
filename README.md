# servomaster

[![Build Status](https://travis-ci.com/climategadgets/servomaster.svg?branch=master)](https://travis-ci.com/climategadgets/servomaster)

## Platform and Hardware Independent Servo Controller Driver ##

Servos are commonly used in radio controlled modeling and robotics applications.
More and more vendors are manufacturing hardware servo controllers that can be controlled by a personal computer
or embedded hardware. Initial list consisted of now defunct FerretTronics FT639 & FT 649,
[PhidgetServo](https://www.phidgets.com/), Scott Edwards Electronics, Inc. (Seetron) [SSC](http://www.seetron.com/),
some [PIC16x84](http://en.wikipedia.org/wiki/PIC16x84) based; later came [Pololu](http://www.pololu.com/) and
[Parallax](http://www.parallax.com/); much more are out there now.

However, there is still no unified servo controller interface, almost two decades since this project was created.

This can and is being fixed, right here, since 2000.

## What Exactly Is Offered ##

* Uniform set of platform and hardware independent abstractions representing servos and servo controllers;
* Motion [transitions and transformations](http://servomaster.sourceforge.net/dev/transform.html);
* Way to introspect and perform capabilities discovery on your hardware;
* Generic serial and USB implementation, suitable for extension for virtually any hardware;
* Growing set of [concrete hardware drivers](http://servomaster.sourceforge.net/relnotes/index.html).

## How To Stay In Touch ##

Come and join [DIY Zoning & Home Climate Control Forum](https://groups.google.com/group/home-climate-control/).
