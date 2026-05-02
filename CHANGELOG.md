# Changelog

## [1.5.1](https://github.com/austv-minecraft/ausBlackMarketExtras/compare/v1.5.0...v1.5.1) (2026-05-02)


### Bug Fixes

* **resume:** preserve running auctions on /axda reload restore ([84dcb2f](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/84dcb2f804daa0ec6364492850a83e035b6fdb44))
* **resume:** skip date-range check for fresh state (&lt; 30 min) ([b06868d](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/b06868d5a7336fd3ac117636fade41e72d95df78))

## [1.5.0](https://github.com/austv-minecraft/ausBlackMarketExtras/compare/v1.4.0...v1.5.0) (2026-05-02)


### Features

* **resume:** auction state persistence & auto-resume ([ca07620](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/ca07620885cb30dc3d2e1b53858c5752e6f3fac7))


### Bug Fixes

* **resume:** add startup fallback restore in onEnable ([7e5fdd3](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/7e5fdd385db3b43f51d9d5c12e2a98506ddaa3c0))

## [1.4.0](https://github.com/austv-minecraft/ausBlackMarketExtras/compare/v1.3.0...v1.4.0) (2026-05-02)


### Features

* **cycle:** add building removal and DecentHolograms hologram on cycle start/end ([95a5183](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/95a51833be43be9c648684edb18362457c7d49d5))
* **cycle:** building removal + DecentHolograms hologram on cycle start/end ([198513a](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/198513ae60acaa36771525b1bb3db9f9b5e3e2ba))
* **resume:** add AuctionResumeHandler ([73badea](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/73badea42bd6cece9413f1b70920eedbfeb9f370))
* **resume:** add AuctionStateCapture ([66ba17f](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/66ba17f6faba4c020714280532e4792942c3d20e))
* **resume:** add CommandInterceptListener for /axda reload ([e4ae9ad](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/e4ae9ad3c04bb345ac3c683797fa0a7cb663f767))
* **resume:** add CycleSnapshot record ([929bcd5](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/929bcd502d53d194e7f8617d8746b9772138a0a7))
* **resume:** add CycleStateRepository ([b7a3651](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/b7a365197f0659c8fbe07225d9cc6d06dedc549c))
* **resume:** add PeriodicSaveTask ([4f43384](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/4f43384fae15b20a072dd3a26c54146091abbf60))
* **resume:** add resume clear/status subcommands ([45b5f56](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/45b5f56d907be94c84dc7084baf8433a68a30f90))
* **resume:** auction state persistence & auto-resume ([12d72e2](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/12d72e29339d000b15b3d3226d162e26ab572ada))
* **resume:** delegate to AuctionResumeHandler in load listener ([0fb515b](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/0fb515b041a0dfd251768976677d97c87b2561a9))
* **resume:** hook state save into AuctionHandler ([6125747](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/6125747eff97118b217c120b5aacedf212ce3e66))
* **resume:** wire all components in plugin lifecycle ([7b0cb9b](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/7b0cb9bc112d47d14d076ddd04200fe6c9db6567))


### Bug Fixes

* **resume:** add fallback restore after /axda reload + merge main ([41d0a79](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/41d0a792e497947b1f12f83aff03c61a8aea7aeb))
* **resume:** address code review findings ([9c33eba](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/9c33eba63fa45085d75ce8f96b28a9a715ebc223))

## [1.3.0](https://github.com/austv-minecraft/ausBlackMarketExtras/compare/v1.2.1...v1.3.0) (2026-04-29)


### Features

* dispara nova release ([96dcd6f](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/96dcd6f91e47bc2e7a70b37149f03fb01dd869be))
* dispara nova release ([96dcd6f](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/96dcd6f91e47bc2e7a70b37149f03fb01dd869be))
* dispara nova release ([a2f12f2](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/a2f12f25881668a59eb607acd8c5ce1e450a931d))

## [1.2.1](https://github.com/austv-minecraft/ausBlackMarketExtras/compare/v1.2.0...v1.2.1) (2026-04-29)


### Bug Fixes

* **commands:** migrate to Paper LifecycleEvents API for command registration ([f685dea](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/f685deafc9c6b6e17db8d99a0472e84fa9a7b54d))
* **commands:** migrate to Paper LifecycleEvents API for command registration ([90e41f2](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/90e41f292aa7f0e8ea43f83c93eff0b940eb4519))

## [1.2.0](https://github.com/austv-minecraft/ausBlackMarketExtras/compare/v1.1.0...v1.2.0) (2026-04-28)


### Features

* add global broadcast with title and sound on cycle start/stop ([addfb38](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/addfb382d489a1262b7df7b37402488e4a55b44b))
* add model records, AusCycleLogger, EmbedConfig and DiscordWebhook with tests ([0784137](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/0784137b701b936b9954df5f15d56be4ca0bb817))
* **commands:** add /ausblack test commands for cycle and reload ([a2f445b](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/a2f445bd292c3f2f2beb4ed549a64a83f209d3ba))
* **commands:** add /ausblack test commands for cycle and reload ([47ca10a](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/47ca10ad2c37b7bd81efe1db76e94bf1eed01362))
* complete plugin implementation with all handlers and main class ([97a36f6](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/97a36f600827a790cf0e7e0253d5750f14f2477b))
* implement ausBlackMarketingExtras auction cycle automation ([c163adf](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/c163adf419fa23ebf6b456313c3f92bae1c01b3c))
* merge broadcast-cycle — chat, title e som ao iniciar/encerrar ciclo ([faf632b](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/faf632beb8c1cd613600b9e3889a6e1e213d9611))


### Bug Fixes

* ensure NPC entity removal and correct schematic restore position ([29ddc86](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/29ddc867d1adaa98b930bcfbc901a4f79a65e5bd))
* replace deprecated SPONGE_SCHEMATIC with SPONGE_V3_SCHEMATIC ([16845b6](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/16845b632e7f4b4d39e32b3c183798813e748f54))
* teleport NPC if already spawned instead of calling spawn() again ([5b80278](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/5b8027845f2cbeec8d983695a2d469a9f84ef065))
* use cycle coordinate from config instead of auction.getSpawn() ([2f458dd](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/2f458dd0411d44b2d99d033bff1088b3848f71c0))


### Documentation

* add broadcast-cycle feature spec with chat, title, and sound ([2e2d2ae](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/2e2d2ae6ed2d0eb6125e36f0d2492a3a0bb9148d))
* add design spec for ausBlackMarketingExtras ([c53355f](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/c53355fe420c3e558fc075994dc6dc684b83d941))
* add implementation plan for ausBlackMarketingExtras ([59ce78d](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/59ce78d35f0ff094efebe8070f90ecd0fcf6ac74))
* add logging system (AusCycleLogger) to design spec ([501bf3e](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/501bf3eaa265c50afacf0f270b9fc2427975afdb))
* update spec to reflect coordinate-based cycle design ([84d3e3b](https://github.com/austv-minecraft/ausBlackMarketExtras/commit/84d3e3bb9ed7baf2a4a8a36458daafb1ae675434))
