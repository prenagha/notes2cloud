#!/usr/bin/perl
# Fix bad IMAP date headers
# Fix stupid <br> at end of every line
# Decode html entities like &gt;
use File::Find;
use HTML::Entities;

find(\&fix, $ARGV[0]);

sub fix {
  $file=$_;
  $path=$File::Find::name;
  $new="";

  if (($file =~ /-notes/) && ($file =~ /^1/)) {
    ($dev,$ino,$mode,$nlink,$uid,$gid,$rdev,$size,
      $atime,$mtime,$ctime,$blksize,$blocks) = stat($path);

    open F, $path or die "Couldn't open $path\n";
    while ($line = <F>) {
      chomp $line;
      if ($line =~ /^Date:\s.*-/) {
        $line =~ s/-/ /g;
      }
      if ($line =~ /<br>$/i) {
        $line =~ s/<br>$//i;
      }
      $new.=decode_entities($line)."\n";
    }
    close F;
    
    open W, ">$path" or die "Couldn't open for write $path\n";
    print W $new;
    close W;
    utime $mtime, $mtime, $path || die "Couldn't utime $path: $! \n";
  }
}
