class BragLogBuddy < Formula
  desc "A command line tool for journaling daily accomplishments"
  homepage "https://github.com/Xenexes/BragLogBuddy"
  url "https://github.com/Xenexes/BragLogBuddy/releases/download/vVERSION/BragLogBuddy-VERSION.jar"
  sha256 "SHA256_HASH_HERE"
  license "MIT"
  version "VERSION"

  depends_on "openjdk@21"

  def install
    libexec.install "BragLogBuddy-#{version}.jar"
    (bin/"BragLogBuddy").write <<~EOS
      #!/bin/bash
      exec "#{Formula["openjdk@21"].opt_bin}/java" -jar "#{libexec}/BragLogBuddy-#{version}.jar" "$@"
    EOS
  end

  test do
    system "#{bin}/BragLogBuddy", "--help"
  end
end