#!/bin/sh

mkdir -p ~/.mozilla/native-messaging-hosts ~/.config/google-chrome/NativeMessagingHosts ~/.config/chromium/NativeMessagingHosts
cp deployment/manifest-firefox.json.template ~/.mozilla/native-messaging-hosts/tabs.json
cp manifest-chrome-stage.json ~/.config/google-chrome/NativeMessagingHosts/tabs.json
cp manifest-chrome-stage.json ~/.config/chromium/NativeMessagingHosts/tabs.json
