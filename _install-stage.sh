#!/bin/sh

mkdir -p ~/.mozilla/native-messaging-hosts ~/.config/google-chrome/NativeMessagingHosts ~/.config/chromium/NativeMessagingHosts
cp deployment/manifest-firefox.json ~/.mozilla/native-messaging-hosts/tabs.json
cp deployment/manifest-chrome.json.template ~/.config/google-chrome/NativeMessagingHosts/tabs.json
cp deployment/manifest-chrome.json.template ~/.config/chromium/NativeMessagingHosts/tabs.json
