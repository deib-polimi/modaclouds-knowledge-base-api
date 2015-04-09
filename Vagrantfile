VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.define "vm-4-kbapi-IT" do |config|
    
    config.vm.box = "ubuntu/trusty64"
    config.vm.hostname = "kb-IT-vm"
    config.vm.network "forwarded_port", guest: 3030, host: 3030

    config.vm.provision "shell", path: "scripts/bootstrap.sh"
    config.vm.provision "shell", run: "always", path: "scripts/startup.sh"

    # http://fgrehm.viewdocs.io/vagrant-cachier
    if Vagrant.has_plugin?("vagrant-cachier")
      config.cache.scope = :box
    end

  end
end
