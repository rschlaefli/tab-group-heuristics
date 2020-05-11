#!/bin/sh

mkdir -p ~/bin/tabs/
cp -R bin ~/bin/tabs/bin
cp -R lib ~/bin/tabs/lib

mkdir -p ~/.mozilla/native-messaging-hosts ~/.config/google-chrome/NativeMessagingHosts ~/.config/chromium/NativeMessagingHosts
cp -u manifest-firefox.json ~/.mozilla/native-messaging-hosts/tabs.json
cp -u manifest-chrome.json ~/.config/google-chrome/NativeMessagingHosts/tabs.json
cp -u manifest-chrome.json ~/.config/chromium/NativeMessagingHosts/tabs.json
