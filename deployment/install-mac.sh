#!/bin/sh

mkdir -p ~/bin/tabs/
cp -R bin ~/bin/tabs/bin
cp -R lib ~/bin/tabs/lib

mkdir -p ~/Library/Application\ Support/Mozilla/NativeMessagingHosts ~/Library/Application\ Support/Google/Chrome/NativeMessagingHosts ~/Library/Application\ Support/Chromium/NativeMessagingHosts
cp -u manifest-chrome.json ~/Library/Application\ Support/Google/Chrome/NativeMessagingHosts/automated-tab-organization.json
cp -u manifest-chrome.json ~/Library/Application\ Support/Chromium/NativeMessagingHosts/automated-tab-organization.json
