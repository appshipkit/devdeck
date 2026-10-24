# DevDeck needs no keep rules: it doesn't use reflection on its own classes, and its
# components (file provider, screen recording service) are kept through the manifest.
#
# Don't add a blanket -keep for io.devdeck: App info detects R8 by checking whether
# io.devdeck.device.R8Probe was renamed or stripped.
