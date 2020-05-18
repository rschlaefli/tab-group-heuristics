#!/bin/sh

mkdir -p ~/bin/tabs/
cp -R bin ~/bin/tabs/
cp -R lib ~/bin/tabs/
chmod +x ~/bin/tabs/bin/tabs

envsubst < manifest-firefox.json.template > manifest-firefox.json
envsubst < manifest-chrome.json.template > manifest-chrome.json

mkdir -p ~/.mozilla/native-messaging-hosts ~/.config/google-chrome/NativeMessagingHosts ~/.config/chromium/NativeMessagingHosts
cp -u manifest-firefox.json ~/.mozilla/native-messaging-hosts/tabs.json
cp -u manifest-chrome.json ~/.config/google-chrome/NativeMessagingHosts/tabs.json
cp -u manifest-chrome.json ~/.config/chromium/NativeMessagingHosts/tabs.json
