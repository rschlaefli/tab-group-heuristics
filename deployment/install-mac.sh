#!/bin/sh

mkdir -p ~/bin/tabs/
cp -R bin ~/bin/tabs/
cp -R lib ~/bin/tabs/
chmod +x ~/bin/tabs/bin/tabs

envsubst < manifest-firefox.json > manifest-firefox.json
envsubst < manifest-chrome.json > manifest-chrome.json

mkdir -p ~/Library/Application\ Support/Mozilla/NativeMessagingHosts ~/Library/Application\ Support/Google/Chrome/NativeMessagingHosts ~/Library/Application\ Support/Chromium/NativeMessagingHosts
# cp manifest-firefox.json ~/.mozilla/native-messaging-hosts/tabs.json
# just copy as there is no -u flag on mac!
cp manifest-chrome.json ~/Library/Application\ Support/Google/Chrome/NativeMessagingHosts/tabs.json
cp manifest-chrome.json ~/Library/Application\ Support/Chromium/NativeMessagingHosts/tabs.json
