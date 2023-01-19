@echo off

chcp 65001

echo 本程序用于处理python第三方库依赖，应只运行一次

echo 多次运行本程序并无任何作用

echo 请确保你的系统已经安装python

echo 如果未安装python请到python.org下载并安装

echo 确认安装python后请继续

pause
pip install single_source
pip install dataclasses
pip install dataclasses_json
pip install base36
pause