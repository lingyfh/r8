#!/usr/bin/env python3
# Copyright (c) 2017, the R8 project authors. Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

from __future__ import print_function
from glob import glob
from itertools import chain
from os import makedirs
from os.path import exists, join, dirname
from shutil import rmtree
from string import Template
import os
import re
import sys

import utils

JCTFROOT = join(utils.REPO_ROOT, 'third_party', 'jctf')
DESTINATION_DIR = join(utils.REPO_ROOT, 'build', 'generated', 'test', 'java',
    'com', 'android', 'tools', 'r8', 'jctf')
PACKAGE_PREFIX = 'com.google.jctf.test.lib.java.'
RELATIVE_TESTDIR = join('LibTests', 'src', 'com', 'google', 'jctf', 'test',
    'lib', 'java')
TESTDIR = join(JCTFROOT, RELATIVE_TESTDIR)
TEMPLATE = Template(
"""// Copyright (c) 2017, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.r8.jctf.${compilerUnderTest}.${relativePackage};

import org.junit.Test;
import com.android.tools.r8.R8RunArtTestsTest;

/**
 * Auto-generated test for the jctf test:
 * ${name}
 *
 * DO NOT EDIT THIS FILE. EDIT THE HERE DOCUMENT TEMPLATE IN
 * tools/create_jctf_tests.py INSTEAD!
 */
public class ${testClassName} extends R8RunArtTestsTest {

  public ${testClassName}() {
    super("${nameWithoutPackagePrefix}", DexTool.NONE);
  }

  @Test
  public void run${testClassName}() throws Exception {
    // For testing with other Art VMs than the default set the system property
    // 'dex_vm' to the desired VM string (e.g. '4.4.4', see ToolHelper.DexVm)
    runJctfTest(CompilerUnderTest.${compilerUnderTestEnum},
      "$classFile",
      "$name"
    );
  }
}
""")

EXIT_FAILURE = 1
RE_PACKAGE = re.compile('package\\s+(com[^\\s;]*)')

def fix_long_path(p):
  if os.name == 'nt':
    p = ('\\\\?\\' + p).decode('utf-8')
  return p

def file_contains_string(filepath, search_string):
  with open(fix_long_path(filepath)) as f:
    return search_string in f.read()

def read_package_from_java_file(filepath):
  with open(fix_long_path(filepath)) as f:
    for line in f:
      m = RE_PACKAGE.search(line)
      if m:
        return m.groups()[0]
  raise IOError("Can't find package statement in java file: " + filepath)


def generate_test(class_name, compiler_under_test, compiler_under_test_enum,
    relative_package):
  filename = join(DESTINATION_DIR, compiler_under_test,
      relative_package.replace('.', os.sep), class_name + '.java')
  utils.makedirs_if_needed(dirname(filename))

  full_class_name = '{}{}.{}'.format(PACKAGE_PREFIX, relative_package,
      class_name)
  contents = TEMPLATE.substitute(
      compilerUnderTest = compiler_under_test,
      relativePackage = relative_package,
      name = full_class_name,
      testClassName = class_name,
      compilerUnderTestEnum = compiler_under_test_enum,
      classFile = full_class_name.replace('.', '/') + '.class',
      nameWithoutPackagePrefix = '{}.{}'.format(relative_package, class_name))

  with open(fix_long_path(filename), 'w') as f:
    f.write(contents)

def Main():
  if not exists(JCTFROOT):
    print('JCTF test package not found in {}'.format(JCTFROOT),
        file = sys.stderr)
    return EXIT_FAILURE

  for tool in ['d8', 'r8']:
    p = fix_long_path(join(DESTINATION_DIR, tool))
    if exists(p):
      rmtree(p)
    makedirs(p)

  java_files = (chain.from_iterable(glob(join(x[0], '*.java'))
      for x in os.walk(TESTDIR)))

  dot_java_dot = '.java.'

  for f in java_files:
    if not file_contains_string(f, '@Test'):
      continue

    class_name = os.path.splitext(os.path.basename(f))[0]
    assert class_name.find('-') < 0

    package = read_package_from_java_file(f)

    idx = package.find(dot_java_dot)
    assert idx >= 0
    relative_package = package[idx + len(dot_java_dot):]

    generate_test(class_name, 'd8', 'R8_AFTER_D8', relative_package)
    generate_test(class_name, 'r8cf', 'R8CF', relative_package)


if __name__ == '__main__':
  sys.exit(Main())
