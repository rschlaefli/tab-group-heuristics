#!/bin/sh

mkdir -p ~/.mozilla/native-messaging-hosts ~/.config/google-chrome/NativeMessagingHosts ~/.config/chromium/NativeMessagingHosts
cp -u manifest-firefox.json ~/.mozilla/native-messaging-hosts/tabs.json
cp -u manifest-chrome.json ~/.config/google-chrome/NativeMessagingHosts/tabs.json
cp -u manifest-chrome.json ~/.config/chromium/NativeMessagingHosts/tabs.json
