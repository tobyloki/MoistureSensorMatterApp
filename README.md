# Moisture Sensor Matter App

- based on v1.4.1 of the Google Home Matter Android SDK at https://github.com/google-home/sample-app-for-matter-android
  - now updated to be based on commit 906db51258c3cfec9608c1b647ec3fc598c3bcfe from main which is 3 commits ahead of v1.4.1
  - 6/3/23
    - incorporated commit 5e5b1d0930eb99e7ad95b8260d770a8424f37d9e
      - remove device from fabric
    - incorporated commit c7bedccfaf31affa75b015caad31d40b2e16e302
      - Handle possible errors when calling device to unlink app's fabric, and fix
    - incorporated commit dcc0377c26150f8387a2955844bfaa9f16892339
      - generate random numbers for device identifiers
        - however, still used option for incremental id instead of random when passing in argument to getNextDeviceId()
    - incorporated commit dd887808689cec1086fcfe97b6f5226693c82b84
      - write node label attribute on commission success
    - incorporated commit 2635112cb082465a919b879563dd98640ca79433
      - check and reset the commissioning window before device sharing

# NOTE
- ensure VPN is disabled before using (required in order for Matter to work)
- if app doesn't work, usually just check the most recent commits to see what's changed & update chip libraries by copying latest third_party folder
